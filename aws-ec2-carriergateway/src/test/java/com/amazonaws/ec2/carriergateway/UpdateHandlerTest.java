package com.amazonaws.ec2.carriergateway;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import org.mockito.Mockito;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends TestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_NoChanges_Success() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);
        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateCreateOnlyProperties_Fails() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);
        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final ResourceModel model = ResourceModel
                .builder()
                .vpcId("vpc-09876543210987654")
                .carrierGatewayId(CAGW_ID)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Cannot update not updatable property VpcId");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void handleRequest_UpdateReadOnlyProperties_Fails() {
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId("vpc-09876543210987654")
                .carrierGatewayId(CAGW_ID)
                .build();
        final ResourceModel desiredModel = ResourceModel.builder()
                .vpcId("vpc-09876543210987654")
                .carrierGatewayId("invalid")
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .desiredResourceState(desiredModel)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Cannot update not updatable property CarrierGatewayId");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
    }

    @Test
    public void handleRequest_UpdateCagwNotFound_Fails() {
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId("vpc-09876543210987654")
                .build();
        final ResourceModel desiredModel = ResourceModel.builder()
                .vpcId("vpc-09876543210987654")
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(model)
                .desiredResourceState(desiredModel)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("CarrierGateway not found");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_TagUpdateNotStarted_InProgress() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW_WITH_TAGS);
        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("Name").value("MyCagw").build());
        newTags.add(Tag.builder().key("Stage").value("Test").build());
        newTags.add(Tag.builder().key("NewKey").value("NewValue").build());

        final ResourceModel model = ResourceModel
                .builder()
                .vpcId(VPC_ID)
                .carrierGatewayId(CAGW_ID)
                .tags(newTags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final Set<Tag> expectedTagsToCreate = new HashSet<>();
        expectedTagsToCreate.add(Tag.builder().key("Stage").value("Test").build());
        expectedTagsToCreate.add(Tag.builder().key("NewKey").value("NewValue").build());
        final Set<Tag> expectedTagsToDelete = new HashSet<>();
        expectedTagsToDelete.add(Tag.builder().key("Stage").value("Prod").build());


        final CallbackContext expectedContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(expectedTagsToCreate)
                .tagsToDelete(expectedTagsToDelete)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(expectedContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStarted_Success() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final Set<Tag> oldTags = new HashSet<>();
        oldTags.add(Tag.builder().key("ThisIsOld").value("OldValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId(VPC_ID)
                .carrierGatewayId(CAGW_ID)
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(oldTags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final CreateTagsRequest expectedCreateTagsRequest = new CreateTagsRequest()
                .withResources(CAGW_ID)
                .withTags(new com.amazonaws.services.ec2.model.Tag().withKey("ThisIsNew").withValue("NewValue"));

        final DeleteTagsRequest expectedDeleteTagsRequest = new DeleteTagsRequest()
                .withResources(CAGW_ID)
                .withTags(new com.amazonaws.services.ec2.model.Tag().withKey("ThisIsOld").withValue("OldValue"));

        verify(proxy)
                .injectCredentialsAndInvoke(eq(expectedCreateTagsRequest), any());
        verify(proxy)
                .injectCredentialsAndInvoke(eq(expectedDeleteTagsRequest), any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStarted_NoTagsToDelete_Success() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId(VPC_ID)
                .carrierGatewayId(CAGW_ID)
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(Collections.emptySet())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final CreateTagsRequest expectedCreateTagsRequest = new CreateTagsRequest()
                .withResources(CAGW_ID)
                .withTags(new com.amazonaws.services.ec2.model.Tag().withKey("ThisIsNew").withValue("NewValue"));

        verify(proxy)
                .injectCredentialsAndInvoke(eq(expectedCreateTagsRequest), any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateStarted_NoTagsToCreate_Success() {
        final Set<Tag> oldTags = new HashSet<>();
        oldTags.add(Tag.builder().key("ThisIsOld").value("OldValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId(VPC_ID)
                .carrierGatewayId(CAGW_ID)
                .tags(Collections.emptySet())
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(Collections.emptySet())
                .tagsToDelete(oldTags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .desiredResourceState(model)
                .build();

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        final DeleteTagsRequest expectedDeleteTagsRequest = new DeleteTagsRequest()
                .withResources(CAGW_ID)
                .withTags(new com.amazonaws.services.ec2.model.Tag().withKey("ThisIsOld").withValue("OldValue"));

        verify(proxy)
                .injectCredentialsAndInvoke(eq(expectedDeleteTagsRequest), any());
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ErrorWhileCreatingTags_Fails() {
        final Set<Tag> newTags = new HashSet<>();
        newTags.add(Tag.builder().key("ThisIsNew").value("NewValue").build());
        final ResourceModel model = ResourceModel
                .builder()
                .vpcId(VPC_ID)
                .carrierGatewayId(CAGW_ID)
                .tags(newTags)
                .build();
        final CallbackContext callbackContext = CallbackContext
                .builder()
                .updateStarted(true)
                .tagsToCreate(newTags)
                .tagsToDelete(Collections.emptySet())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(createModelFromCarrierGateway(TEST_CAGW))
                .previousResourceState(model)
                .desiredResourceState(model)
                .build();

        final AmazonEC2Exception unauthorizedException = new AmazonEC2Exception("");
        unauthorizedException.setErrorCode("UnauthorizedOperation");

        when(proxy.injectCredentialsAndInvoke(any(CreateTagsRequest.class), any())).thenThrow(unauthorizedException);

        final UpdateHandler handler = new UpdateHandler();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, callbackContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
