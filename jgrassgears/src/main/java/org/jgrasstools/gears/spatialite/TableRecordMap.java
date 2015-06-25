/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gears.spatialite;

import java.util.HashMap;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A simple table record representation as hashmap with the column name.
 * 
 * <p>If performance is needed, this should not be used.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TableRecordMap {
    public Geometry geometry;

    public HashMap<String, Object> data = new HashMap<String, Object>();
}
