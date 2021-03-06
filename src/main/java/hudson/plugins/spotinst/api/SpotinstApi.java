package hudson.plugins.spotinst.api;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.plugins.spotinst.api.infra.*;
import hudson.plugins.spotinst.common.SpotinstContext;
import hudson.plugins.spotinst.model.aws.*;
import hudson.plugins.spotinst.model.azure.AzureDetachInstancesRequest;
import hudson.plugins.spotinst.model.azure.AzureGroupInstance;
import hudson.plugins.spotinst.model.azure.AzureGroupInstancesResponse;
import hudson.plugins.spotinst.model.gcp.*;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class SpotinstApi {

    //region Members
    private static final Logger LOGGER                 = LoggerFactory.getLogger(SpotinstApi.class);
    private final static String SPOTINST_API_HOST      = "https://api.spotinst.io";
    private final static String HEADER_AUTH            = "Authorization";
    private final static String AUTH_PREFIX            = "Bearer ";
    private final static String HEADER_CONTENT_TYPE    = "Content-Type";
    private final static String CONTENT_TYPE           = "application/json";
    private final static String QUERY_PARAM_ACCOUNT_ID = "accountId";
    private final static String USER_AGENT_FORMAT      = "Jenkins/%s;spotinst-plugin/%s";
    private final static String PLUGIN_NAME            = "spotinst";
    private final static String HEADER_USER_AGENT      = "User-Agent";
    private final static String QUERY_PARAM_ADJUSTMENT = "adjustment";
    //endregion

    //region Public Methods

    public static int validateToken(String token, String accountId) {
        int                 isValid;
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_AUTH, AUTH_PREFIX + token);
        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE);

        Map<String, String> queryParams = new HashMap<>();

        if (accountId != null && accountId.isEmpty() == false) {
            queryParams.put(QUERY_PARAM_ACCOUNT_ID, accountId);
        }

        try {
            RestResponse response =
                    RestClient.sendGet(SPOTINST_API_HOST + "/events/subscription", headers, queryParams);
            if (response.getStatusCode() == HttpStatus.SC_OK) {
                isValid = 0;
            }
            else if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                isValid = 1;
            }
            else {
                isValid = 2;
            }
        }
        catch (Exception e) {
            isValid = 2;
        }
        return isValid;
    }

    //region AWS
    public static List<AwsGroupInstance> getAwsGroupInstances(String groupId) throws ApiException {
        List<AwsGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>    headers     = buildHeaders();
        Map<String, String>    queryParams = buildQueryParams();

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/status", headers, queryParams);

        AwsGroupInstancesResponse instancesResponse = getCastedResponse(response, AwsGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }

    public static AwsScaleUpResult awsScaleUp(String groupId, int adjustment) throws ApiException {
        AwsScaleUpResult    retVal      = null;
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();
        queryParams.put(QUERY_PARAM_ADJUSTMENT, String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/aws/ec2/group/" + groupId + "/scale/up", null, headers, queryParams);

        AwsScaleUpResponse scaleUpResponse = getCastedResponse(response, AwsScaleUpResponse.class);

        if (scaleUpResponse.getResponse().getItems().size() > 0) {
            retVal = scaleUpResponse.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static Boolean awsDetachInstance(String instanceId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        AwsDetachInstancesRequest request = new AwsDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceId));
        request.setShouldDecrementTargetCapacity(true);
        request.setShouldTerminateInstances(true);

        String body = JsonMapper.toJson(request);

        RestResponse response =
                RestClient.sendPut(SPOTINST_API_HOST + "/aws/ec2/instance/detach", body, headers, queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }
    //endregion

    //region GCP
    public static GcpScaleUpResult gcpScaleUp(String groupId, int adjustment) throws ApiException {

        GcpScaleUpResult    retVal  = null;
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams();
        queryParams.put(QUERY_PARAM_ADJUSTMENT, String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/scale/up", null, headers, queryParams);

        GcpScaleUpResponse scaleUpResponse = getCastedResponse(response, GcpScaleUpResponse.class);

        if (scaleUpResponse.getResponse().getItems().size() > 0) {
            retVal = scaleUpResponse.getResponse().getItems().get(0);
        }

        return retVal;
    }

    public static Boolean gcpDetachInstance(String groupId, String instanceName) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        GcpDetachInstancesRequest request = new GcpDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceName));
        request.setShouldDecrementTargetCapacity(true);
        request.setShouldTerminateInstances(true);
        String body = JsonMapper.toJson(request);

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/detachInstances", body, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }

    public static List<GcpGroupInstance> getGcpGroupInstances(String groupId) throws ApiException {
        List<GcpGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>    headers     = buildHeaders();
        Map<String, String>    queryParams = buildQueryParams();

        RestResponse response =
                RestClient.sendGet(SPOTINST_API_HOST + "/gcp/gce/group/" + groupId + "/status", headers, queryParams);

        GcpGroupInstancesResponse instancesResponse = getCastedResponse(response, GcpGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }
    //endregion

    //region Azure
    public static List<AzureGroupInstance> getAzureGroupInstances(String groupId) throws ApiException {
        List<AzureGroupInstance> retVal      = new LinkedList<>();
        Map<String, String>      headers     = buildHeaders();
        Map<String, String>      queryParams = buildQueryParams();

        RestResponse response = RestClient
                .sendGet(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/status", headers, queryParams);

        AzureGroupInstancesResponse instancesResponse = getCastedResponse(response, AzureGroupInstancesResponse.class);

        if (instancesResponse.getResponse().getItems().size() > 0) {
            retVal = instancesResponse.getResponse().getItems();
        }

        return retVal;
    }

    public static Boolean azureScaleUp(String groupId, int adjustment) throws ApiException {
        Map<String, String> headers = buildHeaders();

        Map<String, String> queryParams = buildQueryParams();
        queryParams.put("adjustment", String.valueOf(adjustment));

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/scale/up", null, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }

    public static Boolean azureDetachInstance(String groupId, String instanceId) throws ApiException {
        Map<String, String> headers     = buildHeaders();
        Map<String, String> queryParams = buildQueryParams();

        AzureDetachInstancesRequest request = new AzureDetachInstancesRequest();
        request.setInstancesToDetach(Arrays.asList(instanceId));
        request.setShouldDecrementTargetCapacity(true);
        String body = JsonMapper.toJson(request);

        RestResponse response = RestClient
                .sendPut(SPOTINST_API_HOST + "/compute/azure/group/" + groupId + "/detachInstances", body, headers,
                         queryParams);

        getCastedResponse(response, ApiEmptyResponse.class);
        Boolean retVal = true;

        return retVal;
    }
    //endregion

    //endregion

    //region Private Methods
    private static String buildUserAgent() {
        String retVal         = null;
        String jenkinsVersion = Jenkins.getInstance().VERSION;
        Plugin spotinst       = Jenkins.getInstance().getPlugin(PLUGIN_NAME);

        if (spotinst != null) {
            PluginWrapper wrapper = spotinst.getWrapper();
            if (wrapper != null) {
                String pluginVersion = wrapper.getVersion();
                retVal = String.format(USER_AGENT_FORMAT, jenkinsVersion, pluginVersion);
            }
        }

        return retVal;
    }

    private static Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_AUTH, AUTH_PREFIX + SpotinstContext.getInstance().getSpotinstToken());
        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE);
        String userAgent = buildUserAgent();

        if (userAgent != null) {
            headers.put(HEADER_USER_AGENT, userAgent);
        }

        return headers;
    }

    private static Map<String, String> buildQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        String              accountId   = SpotinstContext.getInstance().getAccountId();

        if (accountId != null && accountId.isEmpty() == false) {
            queryParams.put(QUERY_PARAM_ACCOUNT_ID, accountId);
        }

        return queryParams;
    }

    private static <T> T getCastedResponse(RestResponse response, Class<T> contentClass) throws ApiException {
        T retVal;

        if (response.getStatusCode() == org.apache.http.HttpStatus.SC_OK) {
            retVal = JsonMapper.fromJson(response.getBody(), contentClass);
            if (retVal == null) {
                throw new ApiException(String.format("Can't parse response to class: %s", contentClass.toString()));
            }

        }
        else {
            String message =
                    String.format("Got status code different then SC_OK : %s. Body: %s", response.getStatusCode(),
                                  response.getBody());
            LOGGER.error(message);

            ApiErrorsResponse apiErrorsResponse = JsonMapper.fromJson(response.getBody(), ApiErrorsResponse.class);
            if (apiErrorsResponse != null) {
                throw new ApiErrorsException(message, apiErrorsResponse.getResponse().getErrors());
            }
            else {
                throw new ApiException(message);
            }
        }

        return retVal;
    }
    //endregion
}
