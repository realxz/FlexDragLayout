package com.example.xiezhen.flexdraglayout;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xiezhen on 2017/7/9.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView rcvMainArticle;
    private MainArticleAdapter mAdapter;
    private FlowDragLayoutManager mLayoutManager;
    private List<String> mArticleList;
    private static final String ARTICLE = "Your relationship shouldn’t have you walking on eggshells around your partner or trying desperately to decode their behavior for a clear message." +
            " In a healthy relationship, there are no mixed signals or broken promises. " +
            "apple banner city dog egg friend great hill";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        loadData();
    }

    private void initView() {
        rcvMainArticle = (RecyclerView) findViewById(R.id.rcv_main_article);
        mLayoutManager = new FlowDragLayoutManager();
        rcvMainArticle.setLayoutManager(mLayoutManager);
        mAdapter = new MainArticleAdapter(this, mArticleList);
        ItemTouchHelper.Callback callback = new FlexItemTouchCallback(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rcvMainArticle);
        rcvMainArticle.setAdapter(mAdapter);
    }

    private void initData() {
        mArticleList = new ArrayList<>();

    }

    private void loadData() {
        String[] essayArray = ARTICLE.split(" ");
        Collections.addAll(mArticleList, essayArray);
        mAdapter.notifyDataSetChanged();
    }


}
