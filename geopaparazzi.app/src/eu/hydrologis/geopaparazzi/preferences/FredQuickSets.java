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
package eu.hydrologis.geopaparazzi.preferences;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;

/**
 * A spinner within preferences, modified from
 * Andrea Antonello's custom sdcard path chooser.
 * 
 * 
 * @author Tim Howard
 *
 */
public class FredQuickSets extends DialogPreference {
    private Context context;
    // private EditText editView;
    private String quicksetChoice = ""; //$NON-NLS-1$
    private Spinner quicksetChoicesSpinner;
    // private List<String> quicksetChoicesList;
    /**
     * @param ctxt  the context to use.
     * @param attrs attributes.
     */
    public FredQuickSets( Context ctxt, AttributeSet attrs ) {
        super(ctxt, attrs);
        this.context = ctxt;
        setPositiveButtonText(ctxt.getString(android.R.string.ok));
        setNegativeButtonText(ctxt.getString(android.R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout mainLayout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(10, 10, 10, 10);
        mainLayout.setLayoutParams(layoutParams);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        TextView comboLabelView = new TextView(context);
        comboLabelView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        comboLabelView.setPadding(2, 2, 2, 2);
        comboLabelView.setText(R.string.fred_choose_settings);
        mainLayout.addView(comboLabelView);

        quicksetChoicesSpinner = new Spinner(context);
        quicksetChoicesSpinner
                .setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        quicksetChoicesSpinner.setPadding(15, 5, 15, 5);
        mainLayout.addView(quicksetChoicesSpinner);

        //quicksetChoicesList.add(0, ""); //$NON-NLS-1$
        ArrayList<String> quicksetChoicesList = new ArrayList<String>();
        // List<String> quicksetChoicesList = new List<String>();
        quicksetChoicesList.add("iMap FDCT"); //$NON-NLS-1$
        quicksetChoicesList.add("Fred-Ecology"); //$NON-NLS-1$
        quicksetChoicesList.add("Fred-Bot,Zool"); //$NON-NLS-1$

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                quicksetChoicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quicksetChoicesSpinner.setAdapter(adapter);
        if (quicksetChoice != null) {
            for( int i = 0; i < quicksetChoicesList.size(); i++ ) {
                if (quicksetChoicesList.get(i).equals(quicksetChoice.trim())) {
                    quicksetChoicesSpinner.setSelection(i);
                    break;
                }
            }
        }

        return mainLayout;
    }
    /*
    @Override
    protected void onBindDialogView( View v ) {
        super.onBindDialogView(v);

        editView.setText(quicksetChoice);
        if (quicksetChoice != null) {
            for( int i = 0; i < quicksetChoicesList.size(); i++ ) {
                if (quicksetChoicesList.get(i).equals(quicksetChoice.trim())) {
                    quicksetChoicesSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void onDialogClosed( boolean positiveResult ) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            quicksetChoice = editView.getText().toString().trim();
            if (quicksetChoice.length() == 0) {
                // try combo
                quicksetChoice = quicksetChoicesSpinner.getSelectedItem().toString();
            }
            if (callChangeListener(quicksetChoice)) {
                persistString(quicksetChoice);
            }
        }
    }
    */
    @Override
    protected Object onGetDefaultValue( TypedArray a, int index ) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue( boolean restoreValue, Object defaultValue ) {

        if (restoreValue) {
            if (defaultValue == null) {
                quicksetChoice = getPersistedString(""); //$NON-NLS-1$
            } else {
                quicksetChoice = getPersistedString(defaultValue.toString());
            }
        } else {
            quicksetChoice = defaultValue.toString();
        }
    }
}