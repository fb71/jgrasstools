package org.jgrasstools.gears;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.gears.modules.TestCountourLinesLabeler;
import org.jgrasstools.gears.modules.TestCoverageConverter;
import org.jgrasstools.gears.modules.TestCoverageSummary;
import org.jgrasstools.gears.modules.TestCutout;
import org.jgrasstools.gears.modules.TestFeatureFilter;
import org.jgrasstools.gears.modules.TestFeatureReshaper;
import org.jgrasstools.gears.modules.TestMapcalc;
import org.jgrasstools.gears.modules.TestMarchingSquaresAndRasterizer;
import org.jgrasstools.gears.modules.TestRasterCatToFeatureAttribute;
import org.jgrasstools.gears.modules.TestReprojectors;
import org.jgrasstools.gears.modules.TestSourceDirection;
import org.jgrasstools.gears.modules.io.TestAdigeBoundaryConditions;
import org.jgrasstools.gears.modules.io.TestEiCalculatorInputOutput;
import org.jgrasstools.gears.modules.io.TestId2ValueConverters;
import org.jgrasstools.gears.modules.io.TestId2ValueReader;
import org.jgrasstools.gears.modules.io.TestRasterReader;
import org.jgrasstools.gears.modules.io.TestShapefileIO;
import org.jgrasstools.gears.modules.io.TestVegetationLibraryReader;

public class FullTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        // IO
        suite.addTestSuite(TestAdigeBoundaryConditions.class);
        suite.addTestSuite(TestEiCalculatorInputOutput.class);
        suite.addTestSuite(TestId2ValueConverters.class);
        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestRasterReader.class);
        suite.addTestSuite(TestShapefileIO.class);
        suite.addTestSuite(TestVegetationLibraryReader.class);

        // modules
        suite.addTestSuite(TestCountourLinesLabeler.class);
        suite.addTestSuite(TestCoverageConverter.class);
        suite.addTestSuite(TestCoverageSummary.class);
        suite.addTestSuite(TestCutout.class);
        suite.addTestSuite(TestFeatureFilter.class);
        suite.addTestSuite(TestFeatureReshaper.class);
        suite.addTestSuite(TestMapcalc.class);
        suite.addTestSuite(TestMarchingSquaresAndRasterizer.class);
        suite.addTestSuite(TestRasterCatToFeatureAttribute.class);
        suite.addTestSuite(TestReprojectors.class);
        suite.addTestSuite(TestSourceDirection.class);

        // other
        suite.addTestSuite(TestInterpolators.class);
        suite.addTestSuite(TestNumericUtilities.class);

        return suite;
    }
}
