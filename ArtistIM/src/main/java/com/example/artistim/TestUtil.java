package com.example.artistim;

import android.util.Log;

public class TestUtil {
    private static volatile TestUtil instance = null;

    public TestUtil getInstance(){
        if(instance==null){
            synchronized (TestUtil.class){
                if(instance == null){
                    instance = new TestUtil();
                }

            }
        }
        return instance;
    }

    private TestUtil(){

    }

    public void haha(){
        Log.d("jjyyjjyys", "haha: ");
    }
}
