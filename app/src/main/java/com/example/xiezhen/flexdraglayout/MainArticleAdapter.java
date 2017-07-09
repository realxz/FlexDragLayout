package com.example.xiezhen.flexdraglayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by xiezhen on 17/7/9.
 */
public class MainArticleAdapter extends RecyclerView.Adapter<MainArticleAdapter.ViewHolder> implements FlexItemTouchCallback.ItemTouchHelperListener {

    private List<String> articleList;
    private Context context;

    public MainArticleAdapter(Context context, List<String> articleList) {
        this.articleList = articleList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_main_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final String word = articleList.get(position);
        holder.tvItemArticle.setText(word);
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }

    @Override
    public void onMove(int fromPosition, int toPosition) {
        final String fromPositionString = articleList.remove(fromPosition);
        articleList.add(toPosition, fromPositionString);
        notifyItemMoved(fromPosition, toPosition);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvItemArticle;

        ViewHolder(View itemView) {
            super(itemView);
            this.tvItemArticle = (TextView) itemView.findViewById(R.id.tv_item_main_article);
        }
    }

}
