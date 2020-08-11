package com.amazonaws.ec2.carriergateway;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateCarrierGatewayRequest;
import com.amazonaws.services.ec2.model.CreateCarrierGatewayResult;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysRequest;
import com.amazonaws.services.ec2.model.DescribeCarrierGatewaysResult;
import org.mockito.Mockito;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
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

import static com.amazonaws.ec2.carriergateway.Constants.POLLING_DELAY_SECONDS;
import static com.amazonaws.ec2.carriergateway.Translator.createModelFromCarrierGateway;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends TestBase {


    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private final ResourceModel model = ResourceModel.builder()
            .vpcId(VPC_ID)
            .build();

    private final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

    private final ResourceHandlerRequest<ResourceModel> requestAfterCagwCreated = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(createModelFromCarrierGateway(TEST_CAGW))
            .build();

    private final CallbackContext inProgressContext = CallbackContext.builder()
            .createStarted(true)
            .build();

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_CreateNotStarted_InProgress() {
        final CreateCarrierGatewayResult createResult = new CreateCarrierGatewayResult()
                .withCarrierGateway(TEST_CAGW);

        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any()))
                .thenReturn(createResult);
        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy)
                .injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateNotStarted_Failed() {
        final AmazonEC2Exception unexpectedException = new AmazonEC2Exception("");
        unexpectedException.setErrorCode("UnexpectedError");

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any()))
                .thenThrow(unexpectedException);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_CreateWithTagsNotStarted_Success() {
        final CreateCarrierGatewayResult createResult = new CreateCarrierGatewayResult()
                .withCarrierGateway(TEST_CAGW_WITH_TAGS);

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any()))
                .thenReturn(createResult);

        final CreateHandler handler = new CreateHandler();

        final Set<Tag> tagSet = new HashSet<>();
        tagSet.add(Tag.builder().key("Name").value("MyCagw").build());
        tagSet.add(Tag.builder().key("Stage").value("Prod").build());

        final ResourceModel modelWithTags = ResourceModel.builder()
                .vpcId(VPC_ID)
                .tags(tagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> requestWithTags = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(modelWithTags)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestWithTags, null, logger);

        verify(proxy)
                .injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW_WITH_TAGS));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CagwAlreadyExists_Fails() {
        final CreateHandler handler = new CreateHandler();

        final AmazonEC2Exception alreadyExistsException = new AmazonEC2Exception("");
        alreadyExistsException.setErrorCode("CarrierGatewayAlreadyExists");


        when(proxy.injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any()))
                .thenThrow(alreadyExistsException);

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CreateStarted_Success() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(TEST_CAGW);

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any());

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
    public void handleRequest_CreateStarted_Failed() {
        final AmazonEC2Exception unexpectedException = new AmazonEC2Exception("");
        unexpectedException.setErrorCode("UnexpectedError");

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenThrow(unexpectedException);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, inProgressContext, logger);

        verify(proxy, times(0))
                .injectCredentialsAndInvoke(any(CreateCarrierGatewayRequest.class), any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(model);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_CagwNotFound_InProgress() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(Collections.emptyList());

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestAfterCagwCreated, inProgressContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(TEST_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CagwPending_InProgress() {
        final DescribeCarrierGatewaysResult describeResult = new DescribeCarrierGatewaysResult()
                .withCarrierGateways(PENDING_CAGW);

        Mockito.lenient().when(proxy.injectCredentialsAndInvoke(any(DescribeCarrierGatewaysRequest.class), any()))
                .thenReturn(describeResult);

        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestAfterCagwCreated, inProgressContext, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isEqualTo(inProgressContext);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(POLLING_DELAY_SECONDS);
        assertThat(response.getResourceModel()).isEqualTo(createModelFromCarrierGateway(PENDING_CAGW));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InvalidRequest_Failed() {
        final CreateHandler handler = new CreateHandler();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, requestAfterCagwCreated, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(requestAfterCagwCreated.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Cannot set read-only property CarrierGatewayId");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
