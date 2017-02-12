/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.plugins.defaultexports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.File;

import eu.geopaparazzi.core.ui.dialogs.GpxExportDialogFragment;
import eu.geopaparazzi.core.ui.dialogs.GpxImportDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileTypes;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExportGpxMenuEntry extends MenuEntry {

    private final Context serviceContext;

    public ExportGpxMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.gpx);
    }

    @Override
    public void onClick(IActivitySupporter clickActivityStarter) {
        GpxExportDialogFragment gpxExportDialogFragment = GpxExportDialogFragment.newInstance(null);
        gpxExportDialogFragment.show(clickActivityStarter.getSupportFragmentManager(), "gpx export");
    }
}
