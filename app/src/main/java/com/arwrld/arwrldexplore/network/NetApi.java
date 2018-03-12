package com.arwrld.arwrldexplore.network;

import com.arwrld.arwrldexplore.utils.Constants;
import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class NetApi {

    public static void fetchPhaseOneProps(FindCallback<ParseObject> callback){
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.DB_PHASE_1);
        query.findInBackground(callback);
    }

}
