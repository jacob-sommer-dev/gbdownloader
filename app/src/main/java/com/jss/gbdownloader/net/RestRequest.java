/**
 *     Copyright 2020 Jacob Sommer
 *
 *     This file is part of gbdownloader.
 *
 *     gbdownloader is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     gbdownloader is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with gbdownloader.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jss.gbdownloader.net;

public abstract class RestRequest  implements Runnable {

    protected String url;
    protected RestRequest.ReqMethod method;
    protected int readTimeout;
    protected int connectTimeout;
    protected RestCallback callback;

    protected RestRequest(Builder builder){
        url = builder.url;
        method = builder.method;
        readTimeout = builder.readTimeout;
        connectTimeout = builder.connectTimeout;
        callback = builder.callback;
    }

    public enum ReqMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        OPTIONS,
        TRACE;
    }

    public static class RestResult{
        public int resultCode;
        public String resultMessage;

        public RestResult(int resultCode, String resultMessage){
            this.resultCode = resultCode;
            this.resultMessage = resultMessage;
        }
    }

    /**
     * Builder for the RestRequest. URL and request method are required params.
     */
    public static abstract class Builder {

        protected String url;
        protected RestRequest.ReqMethod method;
        protected int readTimeout = 10000;
        protected int connectTimeout = 10000;
        protected RestCallback callback;

        /**
         * Constructor for the RestRequest builder. URL and request method are required params.
         * @param url String representation of the RESt endpoint url.
         * @param method The request method to use.
         */
        public Builder(String url, ReqMethod method, RestCallback callback){
            this.url = url;
            this.method = method;
            this.callback = callback;
        }

        public Builder setUrl(String url){
            this.url = url;
            return this;
        }

        public Builder setRequestMethod(VidListRestRequest.ReqMethod method){
            this.method = method;
            return this;
        }

        public Builder setReadTimeout(int timeout){
            readTimeout = timeout;
            return this;
        }

        public Builder setConnectTimeout(int timeout){
            connectTimeout = timeout;
            return this;
        }

        public abstract RestRequest build();

    }

    public interface RestCallback {
        void OnResult(RestResult result);
    }

}
