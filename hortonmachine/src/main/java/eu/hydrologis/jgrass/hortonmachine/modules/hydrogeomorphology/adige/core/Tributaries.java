package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core;

import java.util.HashMap;

import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMConstants;

/**
 * Utility class for handling of tributary mappings and data retrival. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Tributaries implements DischargeContributor {

    private final HashMap<String, Integer> tributary_pfaff2idMap;
    private HashMap<Integer, Double> tributary_id2valuesQMap;

    /**
     * Constructor.
     * 
     * @param tributary_pfaff2idMap {@link HashMap map} of pfafstetter numbers versus
     *                      tributary points id.
     * @param tributary_id2valuesQMap map of tributary points id versus discharge value.
     */
    public Tributaries( HashMap<String, Integer> tributary_pfaff2idMap ) {
        this.tributary_pfaff2idMap = tributary_pfaff2idMap;
    }

    public Double getDischarge( String pNum, double inputDischarge ) {
        Integer damId = tributary_pfaff2idMap.get(pNum);
        if (damId != null) {
            Double discharge = tributary_id2valuesQMap.get(damId);
            if (discharge != null) {
                // sum restituzione discharge to the input discharge
                return discharge + inputDischarge;
            }
        }
        return HMConstants.doubleNovalue;
    }

    public void setCurrentData( HashMap<Integer, Double> currentDataMap ) {
        tributary_id2valuesQMap = currentDataMap;
    }

}
