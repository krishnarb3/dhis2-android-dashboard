package org.hisp.dhis.android.dashboard.ui.adapters;

import android.app.DatePickerDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.hisp.dhis.android.dashboard.R;
import org.hisp.dhis.android.dashboard.ui.models.Field;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by arazabishov on 7/27/15.
 */
public class AccountEditFieldAdapter extends AbsAdapter<Field, RecyclerView.ViewHolder> {

    private static final int EDITTEXT=0,SPINNER=1,DATEPICKER=2;
    private static Context context;
    private static final String[] genderList = {"Male", "Female", "Other"};

    public AccountEditFieldAdapter(Context context, LayoutInflater inflater) {
        super(context, inflater);
        AccountEditFieldAdapter.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if(viewType == EDITTEXT) {
            View viewEditText = inflater.inflate(R.layout.recycler_view_edit_profile_item_edittext
                    , parent, false);
            viewHolder = new FieldEditTextViewHolder(viewEditText, new EditTextListener());
        }
        else if(viewType == SPINNER){
            View viewSpinner = inflater.inflate(R.layout.recycler_view_edit_profile_item_spinner
                    , parent, false);
            viewHolder = new FieldSpinnerViewHolder(viewSpinner);
        }
        else{
            View viewDatePicker = inflater.inflate(R.layout.recycler_view_edit_profile_item_textview
                    , parent, false);
            viewHolder = new FieldDatePickerViewHolder(viewDatePicker, new DatePickerListener());
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final Field field;
        switch (holder.getItemViewType()) {
            case EDITTEXT:
                field = getData().get(position);
                final FieldEditTextViewHolder fieldEditTextViewHolder
                        = (FieldEditTextViewHolder) holder;
                fieldEditTextViewHolder.mEditTextListener.updatePosition(position);
                fieldEditTextViewHolder.labelTextView.setText(field.getLabel());
                fieldEditTextViewHolder.valueEditText.setText(field.getValue());

                if(field.getLabel().equals("Email")) {
                    fieldEditTextViewHolder.mEditTextListener.setEditText(
                            fieldEditTextViewHolder.valueEditText);
                    if(!isValidEmail(field.getValue())) {
                        fieldEditTextViewHolder.valueEditText.setError("Invalid Email");
                    }
                }
                else
                    fieldEditTextViewHolder.valueEditText.setError(null);
                break;
            case SPINNER:
                field = getData().get(position);
                FieldSpinnerViewHolder fieldSpinnerViewHolder = (FieldSpinnerViewHolder) holder;
                fieldSpinnerViewHolder.labelTextView.setText(field.getLabel());

                if (field.getLabel().equals("Gender")) {
                    int pos=0;
                    String gender = field.getValue();
                    if(gender.equals(genderList[0]))
                        pos = 0;
                    else if(gender.equals(genderList[1]))
                        pos = 1;
                    else if(gender.equals(genderList[2]))
                        pos = 2;

                    fieldSpinnerViewHolder.valueSpinner.setSelection(pos);
                    fieldSpinnerViewHolder.valueSpinner.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent
                                , View view, int itemPosition, long id) {
                            List<Field> fieldList = getData();
                            fieldList.set(position
                                    , new Field(getData().get(position).getLabel()
                                    , genderList[itemPosition]));
                            swapData(fieldList);
                            AccountEditFieldAdapter.this.notifyDataSetChanged();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
                break;
            case DATEPICKER:
                field = getData().get(position);
                final FieldDatePickerViewHolder fieldDatePickerViewHolder
                    = (FieldDatePickerViewHolder) holder;
                fieldDatePickerViewHolder.labelTextView.setText(field.getLabel());
                fieldDatePickerViewHolder.valueTextView.setText(field.getValue());

                String birthday = field.getValue();
                String[] date = birthday.split("-");
                final int year = Integer.parseInt(date[0]);
                final int month = Integer.parseInt(date[1]);
                final int day = Integer.parseInt(date[2]);

                fieldDatePickerViewHolder.valueTextView.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                fieldDatePickerViewHolder
                                        .mDatePickerListener.updatePosition(position);
                                new DatePickerDialog(context
                                        , fieldDatePickerViewHolder.mDatePickerListener
                                        , year, month-1, day).show();
                            }
                });
                break;

        }
    }

    public static class FieldEditTextViewHolder extends RecyclerView.ViewHolder {

        public EditTextListener mEditTextListener;

        @Bind(R.id.field_label_text_view)
        public TextView labelTextView;

        @Bind(R.id.field_edit_text)
        public EditText valueEditText;

        public FieldEditTextViewHolder(View itemView, EditTextListener mEditTextListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.mEditTextListener = mEditTextListener;
            this.valueEditText.addTextChangedListener(mEditTextListener);
        }
    }

    public static class FieldSpinnerViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.field_label_text_view)
        public TextView labelTextView;

        @Bind(R.id.field_spinner)
        public Spinner valueSpinner;

        public FieldSpinnerViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.valueSpinner.setAdapter(new ArrayAdapter<>(     //For Better Scrolling Performance
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    genderList
            ));

        }
    }

    public static class FieldDatePickerViewHolder extends RecyclerView.ViewHolder {

        public DatePickerListener mDatePickerListener;

        @Bind(R.id.field_label_text_view)
        public TextView labelTextView;

        @Bind(R.id.field_value_text_view)
        public TextView valueTextView;

        public FieldDatePickerViewHolder(View itemView,DatePickerListener mDatePickerListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.mDatePickerListener = mDatePickerListener;
        }
    }

    public class EditTextListener implements TextWatcher {

        EditText mEditText;
        private int position;
        public void updatePosition(int position) {
            this.position = position;
        }
        public void setEditText(EditText mEditText) {    //For setting error without calling onBind
            this.mEditText = mEditText;
        }
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            List<Field> fieldList = getData();
            fieldList.set(position, new Field(
                    getData().get(position).getLabel(), charSequence.toString()));
            swapData(fieldList);
            if(!isValidEmail(charSequence)&&mEditText!=null) {
                mEditText.setError("Invalid Email");
            }
            else if(mEditText!=null){
                mEditText.setError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    public class DatePickerListener implements DatePickerDialog.OnDateSetListener {

        private int position;
        public void updatePosition(int position) {
            this.position = position;
        }
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            String dateSet;

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, monthOfYear, dayOfMonth);
            SimpleDateFormat dateformat = new SimpleDateFormat(
                    "yyyy-MM-dd", Locale.ENGLISH);
            dateSet = dateformat.format(calendar.getTime());
            List<Field> fieldList = getData();
            fieldList.set(position
                    , new Field(getData().get(position).getLabel(), dateSet));
            swapData(fieldList);
            AccountEditFieldAdapter.this.notifyDataSetChanged();
        }
    }
    @Override
    public int getItemViewType(int position) {
        if(getData().get(position).getLabel().equals("Gender"))
            return SPINNER;
        else if(getData().get(position).getLabel().equals("Birthday"))
            return DATEPICKER;
        else
            return EDITTEXT;
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
