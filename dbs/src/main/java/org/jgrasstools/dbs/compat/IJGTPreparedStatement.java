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
package org.jgrasstools.dbs.compat;

import java.sql.ResultSet;

/**
 * Interface wrapping a prep statement.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IJGTPreparedStatement extends AutoCloseable {

    void setString( int index, String text ) throws Exception;

    void executeUpdate() throws Exception;

    void setDouble( int index, double value ) throws Exception;

    void setFloat( int index, float value ) throws Exception;

    void setInt( int index, int value ) throws Exception;

    void setBoolean( int index, boolean value ) throws Exception;

    void addBatch() throws Exception;

    int[] executeBatch() throws Exception;

    void setLong( int index, long value ) throws Exception;

    void setBytes( int index, byte[] value ) throws Exception;

    void setShort( int index, short value ) throws Exception;

    ResultSet getGeneratedKeys() throws Exception;

}
