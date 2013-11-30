/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
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
package eu.geopaparazzi.spatialite.util;

import java.io.File;
import java.io.IOException;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import eu.geopaparazzi.library.database.GPLog;

/**
 * SpatialiteUtilities class.
 * Goal is to:
 * - determine which Spatialite Database version is being read
 * - create a new Spatialite Database
 * - convert a sqlite3 Database to a Spatialite Database
 * - convert older spatialite Database to present version
 * -- these spatialite function may not be accessible from sql
 * @author Mark Johnson
 */
public class SpatialiteUtilities {
    // -----------------------------------------------
    /**
      * General Function to create jsqlite.Database with spatialite support
      * - parent diretories will be created, if needed
      * - needed Tables/View and default values for metdata-table will be created
      * @param s_db_path name of Database file to create
      * @return sqlite_db: pointer to Database created
      */
    public static Database create_db( String s_db_path)  throws IOException  {
        Database sqlite_db = null;
        File file_db = new File(s_db_path);
        if (!file_db.getParentFile().exists())
        {
         File dir_db = file_db.getParentFile();
         if (!dir_db.mkdir())
         {
          throw new IOException("SpatialiteUtilities: create_db: dir_db[" + dir_db.getAbsolutePath()
                        + "] creation failed");
         }
        }
        sqlite_db = new jsqlite.Database();
        if (sqlite_db != null) {
         try
         {
          sqlite_db.open(file_db.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
          int i_rc=create_spatialite(sqlite_db,0);
         }
         catch (jsqlite.Exception e_stmt)
         {
          GPLog.androidLog(4, "create_spatialite[spatialite] dir_file["+file_db.getAbsolutePath()+"]", e_stmt);
         }
        }
        return sqlite_db;
    }
    // -----------------------------------------------
    /**
      * General Function to create jsqlite.Database with spatialite support
      * - parent diretories will be created, if needed
      * - needed Tables/View and default values for metdata-table will be created
      * @param sqlite_db: pointer to Database
      * @param i_parm: 0=new Database - skip checking if it a patialite Database ; check Spatialite Version
      * @return i_rc: pointer to Database created
      */
    public static int create_spatialite( Database sqlite_db , int i_parm) throws Exception {
     int i_rc=0;
     if (i_parm == 1)
     {
      // 0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0 ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1
      int i_spatialite_version=get_table_fields(sqlite_db,"");
      if (i_spatialite_version > 0)
      { // this is a spatialite Database, do not create
       i_rc=1;
       if (i_spatialite_version != 4)
       { // TODO: logic for convertion to latest Spatialite Version [open]
       }
      }
     }
     if (i_rc == 0)
     {
      String s_sql_command="SELECT InitSpatialMetadata(1)"; // As transaction
      Stmt this_stmt = sqlite_db.prepare(s_sql_command);
      try
      {
       if (this_stmt.step())
       {
       }
      }
      catch (jsqlite.Exception e_stmt)
      {
       GPLog.androidLog(4, "create_spatialite[spatialite] sql["+s_sql_command+"]", e_stmt);
      }
      finally
      {
       if (this_stmt != null)
       {
        this_stmt.close();
       }
      }
     }
     return i_rc;
    }
    // -----------------------------------------------
    /**
      * Goal is to determin the Spatialite version of the Database being used
      * - if (sqlite3_exec(this_handle_sqlite3,"SELECT InitSpatialMetadata()",NULL,NULL,NULL) == SQLITE_OK)
      *  - 'geometry_columns'
      * -- SpatiaLite 2.0 until present version
      * - 'spatial_ref_sys'
      * -- SpatiaLite 2.0 until present version
      * -- SpatiaLite 2.3.1 has no field 'srs_wkt' or 'srtext' field,only 'proj4text' and
      * -- SpatiaLite 2.4.0 first version with 'srs_wkt'
      * -- SpatiaLite 3.1.0-RC2 last version with 'srs_wkt'
      * -- SpatiaLite 4.0.0-RC1 : based on ISO SQL/MM standard 'srtext'
      * -- views: vector_layers_statistics,vector_layers
      * -- SpatiaLite 4.0.0 : introduced
      * 20131129: at the moment not possible to distinguish beteewn 2.4.0 and 3.0.0 [no '2']
      * @param sqlite_db Database connection to use
      * @param s_table name of table to read [if empty: list of tables in Database]
      * @return i_spatialite_version [0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0 ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1]
      */
    private static int get_table_fields(Database sqlite_db, String s_table) throws Exception {
        Stmt this_stmt = null;
        // views: vector_layers_statistics,vector_layers
        boolean b_vector_layers_statistics = false;
        boolean b_vector_layers = false;
        // tables: geometry_columns,raster_columns
        boolean b_geometry_columns = false; // false=not a spatialite Database ; true is a spatialite Database
        int  i_srs_wkt = 0; // 0=not found = pre 2.4.0 ; 1=2.4.0 to 3.1.0 ; 2=starting with 4.0.0
        boolean b_spatial_ref_sys = false;
        int i_spatialite_version=0; // 0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0 ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1
        String s_sql_command = "";
        if (!s_table.equals("")) { // pragma table_info(geodb_geometry)
            s_sql_command = "pragma table_info(" + s_table + ")";
        } else {
            s_sql_command = "SELECT name,type FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        }
        String s_type = "";
        String s_name = "";
        this_stmt = sqlite_db.prepare(s_sql_command);
        try {
            while( this_stmt.step() ) {
                if (!s_table.equals("")) { // pragma table_info(berlin_strassen_geometry)
                    s_name = this_stmt.column_string(1);
                    // 'proj4text' must always exist - otherwise invalid
                    if (s_name.equals("proj4text"))
                        b_spatial_ref_sys = true;
                    if (s_name.equals("srs_wkt"))
                        i_srs_wkt = 1;
                    if (s_name.equals("srtext"))
                        i_srs_wkt = 2;
                }
                if (s_table.equals("")) {
                    s_name = this_stmt.column_string(0);
                    s_type = this_stmt.column_string(1);
                    if (s_type.equals("table")) {
                        if (s_name.equals("geometry_columns")) {
                            b_geometry_columns = true;
                        if (s_name.equals("spatial_ref_sys")) {
                            b_spatial_ref_sys = true;
                        }
                      }
                    }
                    if (s_type.equals("view")) { // SELECT name,type,sql FROM sqlite_master WHERE
                                                 // (type='view')
                           if (s_name.equals("vector_layers_statistics")) {
                            b_vector_layers_statistics = true;
                        }
                        if (s_name.equals("vector_layers")) {
                            b_vector_layers = true;
                        }
                    }
                }
            }
        } finally {
            if (this_stmt != null) {
                this_stmt.close();
            }
        }
        if (s_table.equals("")) {
            if ((b_geometry_columns) && (b_spatial_ref_sys))
            {
               if (b_spatial_ref_sys)
               {
                i_srs_wkt=get_table_fields(sqlite_db,"spatial_ref_sys");
                if ((b_vector_layers_statistics) && (b_vector_layers) && (i_srs_wkt == 4))
                { // Spatialite 4.0
                 i_spatialite_version = 4;
                }
                else
                {
                 if ((!b_vector_layers_statistics) && (!b_vector_layers))
                 { // 'srs_wkt' and missing 'vector_layers_statistics' is not possible - error
                  i_spatialite_version = i_srs_wkt;
                 }
                }
               }
            }
        }
        else
        {
         if (b_spatial_ref_sys)
         { // 'proj4text' must always exist - otherwise invalid
          switch (i_srs_wkt)
          {
           case 0:
            i_spatialite_version=1; // no  'srs_wkt' or 'srtext' fields
           break;
           case 1:
            i_spatialite_version=3; // 'srs_wkt'
           break;
           case 2:
            i_spatialite_version=4; // 'srtext'
           break;
          }
         }
        }
        return i_spatialite_version;
    }
}