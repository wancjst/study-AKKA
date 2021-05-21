package com.iquantex.akka.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author quail
 */
public class JSONObject {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String toJsonString(Object o) throws JsonProcessingException {
        if (o instanceof String){
            return (String) o;
        }
        if (o == null ){
            return null;
        }
        return  mapper.writeValueAsString(o);
    }
}
