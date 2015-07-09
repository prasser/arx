/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.test;

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.PrivacyCriterion;
import org.junit.Test;

/**
 * Test for data transformations.
 *
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public abstract class TestExecutionTimeAbstract extends TestAnonymizationAbstract {

    /**
     * Creates a new instance.
     *
     * @param testCase
     */
    public TestExecutionTimeAbstract(final ARXAnonymizationTestCase testCase) {
        super(testCase);
    }

    /**
     * 
     *
     * @throws IOException
     */
    @Test
    public void test() throws IOException {

        final Data data = getDataObject(testCase);

        // Create an instance of the anonymizer
        final ARXAnonymizer anonymizer = new ARXAnonymizer();
        testCase.config.setPracticalMonotonicity(testCase.practical);

        // Warm up
        anonymizer.anonymize(data, testCase.config);

        // Repeat
        long time = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            data.getHandle().release();
            anonymizer.anonymize(data, testCase.config);
        }
        time = (System.currentTimeMillis() - time) / 5;
        
        System.out.println("Experiment:");
        System.out.println(" - Dataset: " + testCase.dataset);
        System.out.println(" - Practical monotonicity: " + testCase.practical);
        System.out.println(" - Suppression limit: " + testCase.config.getMaxOutliers());
        System.out.println(" - Privacy model: " + getPrivacyModel(testCase.config));
        System.out.println(" - Anonymization performed in: " + time + " [ms]");
    }

    /**
     * Returns a string representing the privacy model
     */
    private String getPrivacyModel(ARXConfiguration config) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        int num = config.getCriteria().size();
        int count = 0;
        for (PrivacyCriterion c : config.getCriteria()) {
            result.append(c.toString());
            if (++count < num) {
                result.append(", ");
            }
        }
        result.append("}");
        return result.toString();
    }
}
