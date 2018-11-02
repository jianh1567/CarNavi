package com.wind.carnavi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.wind.carnavi.adapter.SearchTextAdapter;
import com.wind.carnavi.model.Destination;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by houjian on 2018/6/13.
 */

public class SearchDestionActivity extends Activity implements OnGetSuggestionResultListener, View.OnClickListener {
    private ImageView mDeleteView;
    private EditText mSearchEdit;
    private ListView mSearchResultLv;
    private SuggestionSearch mSuggestionSearch = null;
    private SearchTextAdapter searchAdapter;
    private String mCity;
    private static final String TAG = "SearchDestActivity";
    private List<Destination> dsData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_destion);
        Intent intent = getIntent();
        if(intent.hasExtra("city")){
            mCity = intent.getStringExtra("city");
        }

        initView();
        dsData = new ArrayList<>();
    }

    private void initView(){
        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);

        mDeleteView = (ImageView) findViewById(R.id.search_iv_delete);
        mSearchEdit = (EditText) findViewById(R.id.searchkey);
        mSearchResultLv = (ListView) findViewById(R.id.lv_search_result);

        mSearchEdit.setOnClickListener(this);
        mDeleteView.setOnClickListener(this);

        mSearchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                if(cs.length() < 0){
                    return;
                }

                if(mCity == null || (mCity != null && mCity.isEmpty())){
                    Toast.makeText(SearchDestionActivity.this, R.string.unable_locate_city, Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.i(TAG, "cs.toString() = " + cs.toString());
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(cs.toString()).city(mCity));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSearchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    mSearchResultLv.setVisibility(View.GONE);
                    startSearchDestion();
                }
                return true;
            }
        });

        mSearchResultLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Destination destination = (Destination) parent.getAdapter().getItem(position);
                String text = destination.getContent();
                Log.i(TAG, " text = " + text + " position = " + position);
                mSearchEdit.setText(text);
                mSearchEdit.setSelection(text.length());

                mSearchResultLv.setVisibility(View.GONE);
                startSearchDestion();
            }
        });
    }

    @Override
    public void onGetSuggestionResult(SuggestionResult res) {
        if (res == null || res.getAllSuggestions() == null) {
            return;
        }

        dsData.clear();
        for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
            if (info.key != null) {
                Log.i(TAG, "info.key = " + info.key);
                dsData.add(new Destination(info.key));
            }
        }
        if(searchAdapter == null){
            searchAdapter = new SearchTextAdapter(this, dsData, R.layout.item_destination_list);
            mSearchResultLv.setAdapter(searchAdapter);
        }else {
            searchAdapter.notifyDataSetChanged();
        }
    }


    public void onBack(){
        finish();
    }

    public void onClickSearchBtn(){
        startSearchDestion();
    }

    private void startSearchDestion(){
        //隐藏软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        Intent intent = new Intent();
        String address = mSearchEdit.getText().toString();
        intent.putExtra("addr", address);
        Log.i(TAG, "onItemClick  address = " + address);
        setResult(2, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchkey:
                mSearchResultLv.setVisibility(View.VISIBLE);
                break;
            case R.id.search_iv_delete:
                mSearchEdit.setText("");
                mDeleteView.setVisibility(View.GONE);
                mSearchResultLv.setVisibility(View.GONE);
                break;
        }
    }
}
