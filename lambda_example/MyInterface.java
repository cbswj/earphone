package com.smartfarm.www;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
public interface MyInterface {

    /**
     * Invoke the Lambda function "AndroidBackendLambdaFunction".
     * The function name is the method name.
     *
     * @LambdaFunction(functionName = "CognitoAuthTest")
     */
    @LambdaFunction
    ResponseClass fireDetection(RequestClass request);

}
