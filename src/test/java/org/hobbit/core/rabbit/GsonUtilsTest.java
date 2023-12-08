package org.hobbit.core.rabbit;

import org.hobbit.core.data.ErrorData;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

public class GsonUtilsTest {

    @Test
    public void testErrorData() {
        ErrorData original = new ErrorData();
        original.setErrorType("http://example.org/generalError");
        original.setContainerId("123");

        Gson gson = new Gson();
        ErrorData received;

        received = GsonUtils.deserializeObjectWithGson(gson, GsonUtils.serializeObjectWithGson(gson, original),
                ErrorData.class);
        Assert.assertEquals(original, received);

        original.setLabel("Example label");
        received = GsonUtils.deserializeObjectWithGson(gson, GsonUtils.serializeObjectWithGson(gson, original),
                ErrorData.class);
        Assert.assertEquals(original, received);

        original.setDescription("Some description");
        received = GsonUtils.deserializeObjectWithGson(gson, GsonUtils.serializeObjectWithGson(gson, original),
                ErrorData.class);
        Assert.assertEquals(original, received);

        original.setDetails("A lot of details....");
        received = GsonUtils.deserializeObjectWithGson(gson, GsonUtils.serializeObjectWithGson(gson, original),
                ErrorData.class);
        Assert.assertEquals(original, received);
    }
}
