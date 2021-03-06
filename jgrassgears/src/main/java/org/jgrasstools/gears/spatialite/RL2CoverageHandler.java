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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.spatialite.RasterCoverage;
import org.jgrasstools.gears.modules.r.tmsgenerator.MBTilesHelper;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Handler for a RL2 coverage.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RL2CoverageHandler {

    private ASpatialDb mSpatialiteDb;
    private IJGTConnection mConn;
    private String mTargetEpsg;
    private String mTableName;
    private RasterCoverage rasterCoverage;

    public RL2CoverageHandler( ASpatialDb spatialiteDb, RasterCoverage rasterCoverage ) {
        this.mSpatialiteDb = spatialiteDb;
        this.rasterCoverage = rasterCoverage;
        this.mTableName = rasterCoverage.coverage_name;
        mTargetEpsg = String.valueOf(rasterCoverage.srid);

        mConn = mSpatialiteDb.getConnection();
    }

    public RasterCoverage getRasterCoverage() {
        return rasterCoverage;
    }

    public String getDatabasePath() {
        return mSpatialiteDb.getDatabasePath();
    }

    /**
     * Extract an image from the database.
     * 
     * @param geom3857
     *            the bounding box geometry, supposed to be in epsg:3857
     * @param width
     *            the pixel width of the expected image.
     * @param height
     *            the pixel height of the expected image.
     * @return the {@link BufferedImage}.
     * @throws Exception
     */
    public BufferedImage getRL2Image( Geometry geom, String geomEpsg, int width, int height ) throws Exception {

        String sql;
        if (geomEpsg != null) {
            sql = "select GetMapImageFromRaster('" + mTableName + "', ST_Transform(GeomFromText('" + geom.toText() + "', "
                    + geomEpsg + "), " + mTargetEpsg + ") , " + width + " , " + height
                    + ", 'default', 'image/png', '#ffffff', 0, 80, 1 )";
        } else {
            sql = "select GetMapImageFromRaster('" + mTableName + "', GeomFromText('" + geom.toText() + "') , " + width + " , "
                    + height + ", 'default', 'image/png', '#ffffff', 0, 80, 1 )";
        }

        try (IJGTStatement stmt = mConn.createStatement()) {
            IJGTResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                byte[] bytes = resultSet.getBytes(1);
                if (bytes == null) {
                    return null;
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                return image;
            }
        }
        return null;
    }

    public BufferedImage getRL2ImageForTile( int x, int y, int zoom, int tileSize ) throws Exception {
        double northLL = MBTilesHelper.tile2lat(y, zoom);
        double southLL = MBTilesHelper.tile2lat(y + 1, zoom);
        double westLL = MBTilesHelper.tile2lon(x, zoom);
        double eastLL = MBTilesHelper.tile2lon(x + 1, zoom);

        Coordinate ll = new Coordinate(westLL, southLL);
        Coordinate ur = new Coordinate(eastLL, northLL);

        Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(new com.vividsolutions.jts.geom.Envelope(ll, ur));
        BufferedImage image = getRL2Image(polygon, "4326", tileSize, tileSize);
        return image;
    }

    public static void main( String[] args ) throws Exception {
        try (ASpatialDb db = new GTSpatialiteThreadsafeDb()) {
            db.open("/media/hydrologis/SPEEDBALL/DATI/ORTOFOTO/ortofoto.sqlite");

            List<RasterCoverage> rcList = db.getRasterCoverages(false);
            for( RasterCoverage rasterCoverage : rcList ) {
                System.out.println(rasterCoverage);
            }
        }
    }
}
