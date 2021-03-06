/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.remote.common.internal.api;

import org.phenotips.remote.api.tojson.PatientToJSONConverter;
import org.phenotips.data.Disorder;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;
import org.phenotips.data.similarity.PatientGenotype;
import org.phenotips.data.similarity.internal.DefaultPatientGenotype;
import org.phenotips.remote.common.ApplicationConfiguration;
import org.phenotips.remote.api.ApiConfiguration;
import org.slf4j.Logger;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DefaultPatientToJSONConverter implements PatientToJSONConverter
{
    private final static String PATIENTMATCHING_JSON_FEATUREMATCHES = "featureMatches";
    private final static String PATIENTMATCHING_JSON_CATEGORY       = "category";
    private final static String PATIENTMATCHING_JSON_CATEGORY_ID    = "id";
    private final static String PATIENTMATCHING_JSON_MATCH          = "match";

    //private final static String ERROR_MESSAGE_UNSUPPORTED_JSON_FORMAT = "Unsupported JSON representation of matched features";

    private Logger logger;

    private final String apiVersion;

    private final Pattern hpoTerm; // not static: may be different from api version to api version

    public DefaultPatientToJSONConverter(String apiVersion, Logger logger)
    {
        this.apiVersion = apiVersion;
        this.logger     = logger;

        this.hpoTerm = Pattern.compile("^HP:\\d+$");
    }

    public JSONObject convert(Patient patient, boolean removePrivateData)
    {
        return this.convert(patient, removePrivateData, 0);
    }

    public JSONObject convert(Patient patient, boolean removePrivateData, int includedTopGenes)
    {
        JSONObject json = new JSONObject();

        json.put(ApiConfiguration.JSON_PATIENT_ID, patient.getId());

        // TODO
        JSONObject contactJson = new JSONObject();
        contactJson.put(ApiConfiguration.JSON_CONTACT_NAME,        "PhenomeCentral");
        contactJson.put(ApiConfiguration.JSON_CONTACT_INSTITUTION, "PhenomeCentral");
        contactJson.put(ApiConfiguration.JSON_CONTACT_HREF,        "mailto:matchmaker@phenomecentral.org");
        json.put(ApiConfiguration.JSON_CONTACT, contactJson);

        try {
            json.put(ApiConfiguration.JSON_PATIENT_GENDER, DefaultPatientToJSONConverter.gender(patient));
            // TODO
            //json.putAll(DefaultPatientToJSONConverter.globalQualifiers(patient));
        } catch (Exception ex) {
            // Do nothing. These are optional.
        }

        JSONArray disorders = DefaultPatientToJSONConverter.disorders(patient);
        if (!disorders.isEmpty()) {
            json.put(ApiConfiguration.JSON_DISORDERS, disorders);
        }

        // TODO: rework this part, as Patient may be an instance of a Patient (for outgoing requests) or
        //       RestrictedSimilarityView (for returning replies to incoming requests), and the two
        //       behave differently
        if (removePrivateData) {
            json.put(ApiConfiguration.JSON_FEATURES, this.nonPersonalFeatures(patient));
        } else {
            json.put(ApiConfiguration.JSON_FEATURES, this.features(patient));
        }
        JSONArray genes = removePrivateData ? DefaultPatientToJSONConverter.restrictedGenes(patient, includedTopGenes, logger) :
                                              DefaultPatientToJSONConverter.genes(patient, includedTopGenes, logger);
        if (!genes.isEmpty()) {
            json.put(ApiConfiguration.JSON_GENES, genes);
        }

        return json;
    }

    private JSONArray features(Patient patient)
    {
        JSONArray features = new JSONArray();
        for (Feature patientFeature : patient.getFeatures()) {
            String featureId = patientFeature.getId();
            if (featureId.isEmpty() || !this.hpoTerm.matcher(featureId).matches()) {
                logger.error("Patient feature parser: ignoring term with non-HPO id [{}]", featureId);
                continue;
            }
            JSONObject featureJson = new JSONObject();
            featureJson.put(ApiConfiguration.JSON_FEATURE_ID,       featureId);
            featureJson.put(ApiConfiguration.JSON_FEATURE_OBSERVED, observedStatusToJSONString(patientFeature));

            Map<String, ? extends FeatureMetadatum> metadata = patientFeature.getMetadata();
            FeatureMetadatum ageOfOnset = metadata.get(ApplicationConfiguration.FEATURE_METADATA_AGEOFONSET);
            if (ageOfOnset != null) {
                featureJson.put(ApiConfiguration.JSON_FEATURE_AGE_OF_ONSET, ageOfOnset.getId());
            }
            features.add(featureJson);
        }
        return features;
    }

    private static String observedStatusToJSONString(Feature feature)
    {
        if (feature.isPresent()) {
            return ApiConfiguration.JSON_FEATURE_OBSERVED_YES;
        }
        return ApiConfiguration.JSON_FEATURE_OBSERVED_NO;
    }

    private JSONArray nonPersonalFeatures(Patient patient)
    {
        /* Example of a expected reply which should be parsed for features:
         *
         * QUERY: "features": [ {"id": "HP:0000316", "observed": "yes"},
         *                      {"id": "HP:0004325", "observed": "yes"},
         *                      {"id": "HP:0001999", "observed": "yes"} ]
         *
         * REPLY:
         *  1) "matchabe" patient with the same set of symptoms {"HP:0000316", "HP:0004325", "HP:0001999"}
         *     and two other unmatched feature:
         *
         *   "features": [{"score":0.46,"category":{"id":"HP:0100886",...},"reference":["HP:0000316"],"match":[""]},
         *                {"score":0.44,"category":{"id":"HP:0004323",...},"reference":["HP:0004325"],"match":[""]},
         *                {"score":0.40,"category":{"id":"HP:0000271",...},"reference":["HP:0001999"],"match":[""]},
         *                {"score":0,"category":{"id":"","name":"Unmatched"},"match":["",""]}]
         *
         *  2) "public" patient with {"HP:0004325", "HP:0001999", "HP:0000479"} and two other unmatched features:
         *
         *   "features": [{"score":0.44,"category":{"id":"HP:0004325",...},"reference":["HP:0004325"],"match":["HP:0004325"]},
         *                {"score":0.40,"category":{"id":"HP:0001999",...},"reference":["HP:0001999"],"match":["HP:0001999"]},
         *                {"score":0.22,"category":{"id":"HP:0000478",...},"reference":["HP:0000316"],"match":["HP:0000479"]},
         *                {"score":0,"category":{"id":"","name":"Unmatched"},"match":["HP:0011276","HP:0000505"]}]
         *
         *  For now feature info returned by the patient-network component will be used, in order
         *  not to reinvent the "privacy" wheel. All non-matched features will be returned as
         *  HP:0000118 ("Phenotypic abnormality"), and (given how patient-netowrk component works)
         *  all non-observed features will be ignored.
         */

        Map<String, Integer> featureCounts      = new HashMap<String, Integer>();
        Set<String>          obfuscatedFeatures = new HashSet<String>();
        Set<String>          notMatchedFeatures = new HashSet<String>();

        JSONArray similarityFeaturesJson = patient.toJSON().getJSONArray(PATIENTMATCHING_JSON_FEATUREMATCHES);

        for (Object featureMatchUC : similarityFeaturesJson) {
            JSONObject featureMatch = (JSONObject) featureMatchUC;

            JSONObject featureCategory = featureMatch.optJSONObject(PATIENTMATCHING_JSON_CATEGORY);
            if (featureCategory == null) {
                //FIXME: throw new Exception(ERROR_MESSAGE_UNSUPPORTED_JSON_FORMAT);
                continue;
            }

            String catId = featureCategory.optString(PATIENTMATCHING_JSON_CATEGORY_ID, "");

            // an unmatched feature
            JSONArray featureMatches = featureMatch.optJSONArray(PATIENTMATCHING_JSON_MATCH);
            if (featureMatches == null) {
                // FIXME: need to throw to indicate unsuported format:throw new Exception(ERROR_MESSAGE_UNSUPPORTED_JSON_FORMAT);
                continue;
            }
            for (int i = 0; i < featureMatches.size(); i++)
            {
                String matchFeature = featureMatches.getString(i);

                // if feature id is obfuscated use category Id instead as the best available substitute
                String featureId = matchFeature.isEmpty() ? catId : matchFeature;

                // replace empty features by the most generic generic term,
                // and (possibly) re-format feature ID to the expected output format
                featureId = processFeatureID(featureId);

                if (catId.isEmpty()) {
                    notMatchedFeatures.add(featureId);
                }
                if (matchFeature.isEmpty()) {
                    obfuscatedFeatures.add(featureId);
                }

                Integer count = featureCounts.containsKey(featureId) ? featureCounts.get(featureId) : 0;
                featureCounts.put(featureId, count + 1);
            }
        }

        // convert featuresWithCounts to features
        // note: for now only observed features are supported, so "observed" is hardcoded to "yes" for now
        JSONArray features = new JSONArray();

        for (String featureId : featureCounts.keySet()) {
            if (featureId.isEmpty() || !this.hpoTerm.matcher(featureId).matches()) {
                logger.error("Patient feature parser: ignoring term with non-HPO id [{}]", featureId);
                continue;
            }

            JSONObject featureJson = new JSONObject();
            featureJson.put(ApiConfiguration.JSON_FEATURE_ID,         featureId);
            featureJson.put(ApiConfiguration.JSON_FEATURE_OBSERVED,   ApiConfiguration.JSON_FEATURE_OBSERVED_YES);
            featureJson.put(ApiConfiguration.JSON_FEATURE_MATCHED,    !notMatchedFeatures.contains(featureId));
            featureJson.put(ApiConfiguration.JSON_FEATURE_OBFUSCATED, obfuscatedFeatures.contains(featureId));
            int count = featureCounts.get(featureId);
            if (count > 1) {
                featureJson.put(ApiConfiguration.JSON_FEATURE_COUNT, count);
            }
            features.add(featureJson);
        }

        return features;
    }

    private static String processFeatureID(String featureIdFromMatching)
    {
        if (featureIdFromMatching.isEmpty()) {
            return ApiConfiguration.REPLY_JSON_FEATURE_HPO_MOST_GENERIC_TERM;
        }
        return featureIdFromMatching;
    }

    private static JSONArray disorders(Patient patient)
    {
        JSONArray disorders = new JSONArray();
        for (Disorder disease : patient.getDisorders()) {
            JSONObject disorderJson = new JSONObject();
            disorderJson.put(ApiConfiguration.JSON_DISORDER_ID, disease.getId());
            disorders.add(disorderJson);
        }
        return disorders;
    }

    private static JSONArray genes(Patient patient, int includedTopGenes, Logger logger)
    {
        //logger.error("[request] Getting candidate genes for patient [{}]", patient.getId());

        JSONArray genes = new JSONArray();
        try {
            PatientGenotype genotype = new DefaultPatientGenotype(patient);

            Set<String> candidateGeneNames = new HashSet<String>(genotype.getCandidateGenes());

            if (includedTopGenes > 0 && candidateGeneNames.size() < includedTopGenes) {
                // keep adding top exomiser genes until we have more than we want. Some of those may
                // overlap with explicit candidate genes, Set will take care of that, but we don't
                // know if the size of the collection grew or not without checking
                List<String> topGenes = genotype.getTopGenes(includedTopGenes);
                for (String topGene : topGenes) {
                    candidateGeneNames.add(topGene);
                    if (candidateGeneNames.size() >= includedTopGenes) {
                        break;
                    }
                }
            }

            for (String geneName : candidateGeneNames) {
                JSONObject gene = new JSONObject();
                gene.put(ApiConfiguration.JSON_GENES_GENE_ID, geneName);

                JSONObject nextGene = new JSONObject();
                nextGene.put(ApiConfiguration.JSON_GENES_GENE, gene);

                genes.add(nextGene);
            }
        } catch (Exception ex) {
            logger.error("Error getting genes for patient [{}]: [{}]", patient.getId(), ex);
            return new JSONArray();
        }
        return genes;
    }

    private static JSONArray restrictedGenes(Patient patient, int includedTopGenes, Logger logger)
    {
        //logger.error("[reply] Getting candidate genes for patient [{}]", patient.getId());

        JSONArray genes = new JSONArray();

        try {
            JSONArray orderedPatientGenes = patient.toJSON().optJSONArray("genes");

            if (orderedPatientGenes == null || includedTopGenes <= 0) {
                return genes;
            }

            int useGenes = Math.min(orderedPatientGenes.size(), includedTopGenes);
            for(int i = 0; i < useGenes; ++i){
                JSONObject nextGeneInfo = orderedPatientGenes.optJSONObject(i);
                // no check for null so that if nextGeneInfo is not a JSONObject an error will be logged
                String geneName = nextGeneInfo.optString("gene", null);
                if (geneName != null) {
                    JSONObject gene = new JSONObject();
                    gene.put(ApiConfiguration.JSON_GENES_GENE_ID, geneName);

                    JSONObject nextGene = new JSONObject();
                    nextGene.put(ApiConfiguration.JSON_GENES_GENE, gene);

                    genes.add(nextGene);
                }
          }

        } catch (Exception ex) {
            logger.error("Error getting genes for patient [{}]: [{}]", patient.getId(), ex);
            return new JSONArray();
        }

        return genes;
    }

    private static String gender(Patient patient)
    {
        String rawSex = patient.<ImmutablePair<String, String>>getData("sex").get(0).getRight();
        if (rawSex.toUpperCase().equals("M")) {
            return ApiConfiguration.JSON_PATIENT_GENDER_MALE;
        } if (rawSex.toUpperCase().equals("F")) {
            return ApiConfiguration.JSON_PATIENT_GENDER_FEMALE;
        }
        return ApiConfiguration.JSON_PATIENT_GENDER_OTHER;
    }

    /*private static Map<String, String> globalQualifiers(Patient patient)
    {
        Map<String, String> globalQualifiers = new HashMap<String, String>();
        Map<String, String> remappedGlobalQualifierStrings = new HashMap<String, String>();
        remappedGlobalQualifierStrings.put("global_age_of_onset", "age_of_onset");
        remappedGlobalQualifierStrings.put("global_mode_of_inheritance", "mode_of_inheritance");

        // These are the actual qualifiers, that are remapped to have the keys compliant with the remote JSON standard.
        PatientData<ImmutablePair<String, SolrOntologyTerm>> existingQualifiers =
            patient.<ImmutablePair<String, SolrOntologyTerm>>getData("global-qualifiers");
        if (globalQualifiers != null) {
            for (ImmutablePair<String, SolrOntologyTerm> qualifierPair : existingQualifiers) {
                for (String key : remappedGlobalQualifierStrings.keySet()) {
                    // Could do contains, but is it safe?
                    if (StringUtils.equalsIgnoreCase(qualifierPair.getLeft(), key)) {
                        globalQualifiers.put(remappedGlobalQualifierStrings.get(key), qualifierPair.getRight().getId());
                        break;
                    }
                }
            }
        }
        return globalQualifiers;
    }*/
}
