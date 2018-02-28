package com.arwrld.arwrldexplore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.arwrld.arwrldexplore.R;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class ArWrldViewerFragment extends BaseFragment {

    private View view;
    WebView webView;

    private String url;

    public static ArWrldViewerFragment newInstance(String arg) {
        Bundle args = new Bundle();
        args.putString("ARG", arg);

        ArWrldViewerFragment fragment = new ArWrldViewerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getArguments().getString("ARG");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_arwrld_viewer, container, false);
        webView = view.findViewById(R.id.webview);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    @Override
    public void onResume() {
        super.onResume();
        Answers.getInstance().logContentView(new ContentViewEvent().putContentName("ArWrld.com"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
