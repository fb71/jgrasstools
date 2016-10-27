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
package org.jgrasstools.nww.layers.defaults.spatialite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.gears.io.las.spatialite.LasCell;
import org.jgrasstools.gears.io.las.spatialite.LasCellsTable;
import org.jgrasstools.gears.io.las.spatialite.LasLevel;
import org.jgrasstools.gears.io.las.spatialite.LasLevelsTable;
import org.jgrasstools.gears.io.las.spatialite.LasSource;
import org.jgrasstools.gears.io.las.spatialite.LasSourcesTable;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.TransformationUtils;
import org.jgrasstools.gears.utils.colors.ColorInterpolator;
import org.jgrasstools.gears.utils.colors.ColorTables;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.nww.layers.defaults.NwwLayer;
import org.jgrasstools.nww.layers.defaults.raster.BasicMercatorTiledImageLayer;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.awt.PointTransformation;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * Procedural layer for spatialite las tables folder.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterizedSpatialiteLasLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 512;

    private Coordinate centre;

    public RasterizedSpatialiteLasLayer( String title, ASpatialDb db, Integer tileSize, boolean transparentBackground,
            boolean doIntensity ) throws Exception {
        super(makeLevels(title, tileSize, transparentBackground, db, doIntensity));
        this.layerName = title;
        this.setUseTransparentTextures(true);

        try {
            Envelope tableBounds = db.getTableBounds(LasSourcesTable.TABLENAME);
            SpatialiteGeometryColumns spatialiteGeometryColumns = db.getGeometryColumnsForTable(LasCellsTable.TABLENAME);
            CoordinateReferenceSystem dataCrs = CRS.decode("EPSG:" + spatialiteGeometryColumns.srid);
            CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
            ReferencedEnvelope env = new ReferencedEnvelope(tableBounds, dataCrs);
            ReferencedEnvelope envLL = env.transform(targetCRS, true);

            centre = envLL.centre();
        } catch (Exception e) {
            e.printStackTrace();
            centre = CrsUtilities.WORLD.centre();
        }
    }

    public static boolean isLasDb( ASpatialDb db ) throws Exception {
        return LasSourcesTable.isLasDatabase(db);
    }

    private static LevelSet makeLevels( String title, Integer tileSize, boolean transparentBackground, ASpatialDb db,
            boolean doIntensity ) throws Exception {
        AVList params = new AVListImpl();

        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

        SpatialiteGeometryColumns spatialiteGeometryColumns = db.getGeometryColumnsForTable(LasCellsTable.TABLENAME);
        CoordinateReferenceSystem dataCrs = CRS.decode("EPSG:" + spatialiteGeometryColumns.srid);
        CoordinateReferenceSystem nwwCRS = DefaultGeographicCRS.WGS84;

        List<LasSource> lasSources = LasSourcesTable.getLasSources(db);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        List<Polygon> polList = new ArrayList<>();
        ColorInterpolator colorInterp;
        int lasLevels = 0;
        if (!doIntensity) {
            for( LasSource lasSource : lasSources ) {
                lasLevels = lasSource.levels;
                polList.add(lasSource.polygon);
                min = Math.min(min, lasSource.minElev);
                max = Math.max(max, lasSource.maxElev);
            }
            colorInterp = new ColorInterpolator(ColorTables.elev.name(), min, max, null);
        } else {
            for( LasSource lasSource : lasSources ) {
                lasLevels = lasSource.levels;
                polList.add(lasSource.polygon);
                min = Math.min(min, lasSource.minIntens);
                max = Math.max(max, lasSource.maxIntens);
            }
            colorInterp = new ColorInterpolator(ColorTables.extrainbow.name(), min, max, null);
        }
        final int _lasLevels = lasLevels;

        Geometry sourcesUnionData = CascadedPolygonUnion.union(polList);
        PreparedGeometry preparedCoverage = PreparedGeometryFactory.prepare(sourcesUnionData);

        MathTransform nww2DataTransform = CRS.findMathTransform(nwwCRS, dataCrs);
        MathTransform data2NwwTransform = CRS.findMathTransform(dataCrs, nwwCRS);

        String cacheRelativePath = "rasterized_spatialites/" + title + "-tiles";
        // String urlString = folderFile.toURI().toURL().toExternalForm();
        // params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, finalTileSize);
        params.setValue(AVKey.TILE_HEIGHT, finalTileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        params.setValue(AVKey.NUM_LEVELS, 22);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));

        File cacheRoot = CacheUtils.getCacheRoot();
        final File cacheFolder = new File(cacheRoot, cacheRelativePath);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
        CacheUtils.clearCacheBySourceName(cacheRelativePath);

        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder(){

            public URL getURL( Tile tile, String altImageFormat ) throws MalformedURLException {
                try {
                    int zoom = tile.getLevelNumber() + 3;
                    Sector sector = tile.getSector();
                    double north = sector.getMaxLatitude().degrees;
                    double south = sector.getMinLatitude().degrees;
                    double east = sector.getMaxLongitude().degrees;
                    double west = sector.getMinLongitude().degrees;
                    double centerX = west + (east - west) / 2.0;
                    double centerY = south + (north - south) / 2.0;
                    int[] tileNumber = NwwUtilities.getTileNumber(centerY, centerX, zoom);
                    int x = tileNumber[0];
                    int y = tileNumber[1];

                    Rectangle imageBounds = new Rectangle(0, 0, finalTileSize, finalTileSize);
                    ReferencedEnvelope tileEnvNww = new ReferencedEnvelope(west, east, south, north, DefaultGeographicCRS.WGS84);

                    AffineTransform worldToPixel = TransformationUtils.getWorldToPixel(tileEnvNww, imageBounds);
                    PointTransformation pointTransformation = new PointTransformation(){
                        @Override
                        public void transform( Coordinate src, Point2D dest ) {
                            worldToPixel.transform(new Point2D.Double(src.x, src.y), dest);
                        }
                    };
                    Polygon polygonNww = GeometryUtilities.createPolygonFromEnvelope(tileEnvNww);
                    Geometry polygonData = JTS.transform(polygonNww, nww2DataTransform);

                    int imgType;
                    Color backgroundColor;
                    boolean intersects = preparedCoverage.intersects(polygonData);
                    if (transparentBackground || !intersects) {
                        imgType = BufferedImage.TYPE_INT_ARGB;
                        backgroundColor = new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(), 0);
                    } else {
                        imgType = BufferedImage.TYPE_INT_RGB;
                        backgroundColor = Color.WHITE;
                    }
                    BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, imgType);
                    Graphics2D gr = image.createGraphics();
                    gr.setPaint(backgroundColor);
                    gr.fill(imageBounds);
                    // gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    // RenderingHints.VALUE_ANTIALIAS_ON);

                    if (zoom < 12) {
                        Geometry sourcesUnionNww = JTS.transform(sourcesUnionData, data2NwwTransform);
                        drawSources(db, pointTransformation, sourcesUnionNww, gr);
                    } else if (zoom < 15) {
                        drawLevels(db, colorInterp, pointTransformation, polygonData, gr, _lasLevels, data2NwwTransform, doIntensity);
                    } else if (zoom < 16 && _lasLevels - 1 > 0) {
                        drawLevels(db, colorInterp, pointTransformation, polygonData, gr, _lasLevels - 1, data2NwwTransform, doIntensity);
                    } else if (zoom < 17 && _lasLevels - 2 > 0) {
                        drawLevels(db, colorInterp, pointTransformation, polygonData, gr, _lasLevels - 2, data2NwwTransform, doIntensity);
                    } else if (zoom < 18 && _lasLevels - 3 > 0) {
                        drawLevels(db, colorInterp, pointTransformation, polygonData, gr, _lasLevels - 3, data2NwwTransform, doIntensity);
                    } else if (zoom < 19 && _lasLevels - 4 > 0) {
                        drawLevels(db, colorInterp, pointTransformation, polygonData, gr, _lasLevels - 4, data2NwwTransform, doIntensity);
                    } else {
                        drawCells(db, colorInterp, pointTransformation, polygonData, gr, doIntensity);
                    }

                    File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + x);
                    if (!tileImageFolderFile.exists()) {
                        tileImageFolderFile.mkdirs();
                    }
                    File imgFile = new File(tileImageFolderFile, y + ".png");
                    if (!imgFile.exists()) {
                        ImageIO.write(image, "png", imgFile);
                    }
                    return imgFile.toURI().toURL();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

        });

        return new LevelSet(params);
    }

    private static void drawCells( ASpatialDb db, ColorInterpolator colorInterp, PointTransformation pointTransformation,
            Geometry polygon, Graphics2D gr, boolean doIntensity ) throws Exception {
        int maxPerImage = 100000;
        List<LasCell> lasCells = LasCellsTable.getLasCells(db, null, polygon, true, true, false, false, false, maxPerImage);
        int size = lasCells.size();
        if (size > 0) {
            int jump = size / maxPerImage;
            ShapeWriter sw = new ShapeWriter(pointTransformation);
            for( int i = 0; i < size; i = i + 1 + jump ) {
                LasCell lasCell = lasCells.get(i);
                Shape shape = sw.toShape(lasCell.polygon);
                Color c = colorInterp.getColorFor(doIntensity ? lasCell.avgIntensity : lasCell.avgElev);
                gr.setPaint(c);
                Rectangle bounds = shape.getBounds();
                // System.out.println(bounds.getX() + "/" + bounds.getY() + "//" + c);
                if (bounds.getWidth() < 5) {
                    gr.fillRect(bounds.x, bounds.y, 50, 50);
                } else {
                    gr.fill(shape);
                }
            }
        }
    }

    private static void drawLevels( ASpatialDb db, ColorInterpolator colorInterp, PointTransformation pointTransformation,
            Geometry polygon, Graphics2D gr, int lasLevelsNum, MathTransform data2NwwTransform, boolean doIntensity ) throws Exception {
        int maxPerImage = 100000;
        List<LasLevel> lasLevels = LasLevelsTable.getLasLevels(db, lasLevelsNum, polygon);

        int size = lasLevels.size();
        if (size > 0) {
            int jump = size / maxPerImage;
            ShapeWriter sw = new ShapeWriter(pointTransformation);
            for( int i = 0; i < size; i = i + 1 + jump ) {
                LasLevel lasLevel = lasLevels.get(i);
                Geometry polygonNww = JTS.transform(lasLevel.polygon, data2NwwTransform);
                Shape shape = sw.toShape(polygonNww);
                Color c = colorInterp.getColorFor(doIntensity ? lasLevel.avgIntensity : lasLevel.avgElev);
                gr.setPaint(c);
                Rectangle bounds = shape.getBounds();
                // System.out.println(bounds.getX() + "/" + bounds.getY() + "//" + c);
                if (bounds.getWidth() < 5) {
                    gr.fillRect(bounds.x, bounds.y, 50, 50);
                } else {
                    gr.fill(shape);
                }
            }
        }
    }

    private static void drawSources( ASpatialDb db, PointTransformation pointTransformation, Geometry sourcesUnion,
            Graphics2D gr ) throws Exception {
        ShapeWriter sw = new ShapeWriter(pointTransformation);
        Shape shape = sw.toShape(sourcesUnion);
        gr.setPaint(Color.red);
        gr.draw(shape);
    }

    public String toString() {
        return layerName;
    }

    @Override
    public Coordinate getCenter() {
        return centre;
    }

}
