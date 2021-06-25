package org.jetbrains.r.rinterop;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.31.1)",
    comments = "Source: service.proto")
public final class RPIServiceGrpc {

  private RPIServiceGrpc() {}

  public static final String SERVICE_NAME = "rplugininterop.RPIService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.GetInfoResponse> getGetInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getInfo",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.GetInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.GetInfoResponse> getGetInfoMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.GetInfoResponse> getGetInfoMethod;
    if ((getGetInfoMethod = RPIServiceGrpc.getGetInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetInfoMethod = RPIServiceGrpc.getGetInfoMethod) == null) {
          RPIServiceGrpc.getGetInfoMethod = getGetInfoMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.GetInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GetInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getInfo"))
              .build();
        }
      }
    }
    return getGetInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.BoolValue> getIsBusyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "isBusy",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.BoolValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.BoolValue> getIsBusyMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.BoolValue> getIsBusyMethod;
    if ((getIsBusyMethod = RPIServiceGrpc.getIsBusyMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getIsBusyMethod = RPIServiceGrpc.getIsBusyMethod) == null) {
          RPIServiceGrpc.getIsBusyMethod = getIsBusyMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.BoolValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "isBusy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("isBusy"))
              .build();
        }
      }
    }
    return getIsBusyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.Init,
      org.jetbrains.r.rinterop.CommandOutput> getInitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "init",
      requestType = org.jetbrains.r.rinterop.Init.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.Init,
      org.jetbrains.r.rinterop.CommandOutput> getInitMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.Init, org.jetbrains.r.rinterop.CommandOutput> getInitMethod;
    if ((getInitMethod = RPIServiceGrpc.getInitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getInitMethod = RPIServiceGrpc.getInitMethod) == null) {
          RPIServiceGrpc.getInitMethod = getInitMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.Init, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "init"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.Init.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("init"))
              .build();
        }
      }
    }
    return getInitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getQuitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "quit",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getQuitMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getQuitMethod;
    if ((getQuitMethod = RPIServiceGrpc.getQuitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getQuitMethod = RPIServiceGrpc.getQuitMethod) == null) {
          RPIServiceGrpc.getQuitMethod = getQuitMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "quit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("quit"))
              .build();
        }
      }
    }
    return getQuitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getQuitProceedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "quitProceed",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getQuitProceedMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getQuitProceedMethod;
    if ((getQuitProceedMethod = RPIServiceGrpc.getQuitProceedMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getQuitProceedMethod = RPIServiceGrpc.getQuitProceedMethod) == null) {
          RPIServiceGrpc.getQuitProceedMethod = getQuitProceedMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "quitProceed"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("quitProceed"))
              .build();
        }
      }
    }
    return getQuitProceedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ExecuteCodeRequest,
      org.jetbrains.r.rinterop.ExecuteCodeResponse> getExecuteCodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "executeCode",
      requestType = org.jetbrains.r.rinterop.ExecuteCodeRequest.class,
      responseType = org.jetbrains.r.rinterop.ExecuteCodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ExecuteCodeRequest,
      org.jetbrains.r.rinterop.ExecuteCodeResponse> getExecuteCodeMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ExecuteCodeRequest, org.jetbrains.r.rinterop.ExecuteCodeResponse> getExecuteCodeMethod;
    if ((getExecuteCodeMethod = RPIServiceGrpc.getExecuteCodeMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getExecuteCodeMethod = RPIServiceGrpc.getExecuteCodeMethod) == null) {
          RPIServiceGrpc.getExecuteCodeMethod = getExecuteCodeMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.ExecuteCodeRequest, org.jetbrains.r.rinterop.ExecuteCodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "executeCode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ExecuteCodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ExecuteCodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("executeCode"))
              .build();
        }
      }
    }
    return getExecuteCodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSendReadLnMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sendReadLn",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSendReadLnMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.Empty> getSendReadLnMethod;
    if ((getSendReadLnMethod = RPIServiceGrpc.getSendReadLnMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSendReadLnMethod = RPIServiceGrpc.getSendReadLnMethod) == null) {
          RPIServiceGrpc.getSendReadLnMethod = getSendReadLnMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sendReadLn"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("sendReadLn"))
              .build();
        }
      }
    }
    return getSendReadLnMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getSendEofMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sendEof",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getSendEofMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getSendEofMethod;
    if ((getSendEofMethod = RPIServiceGrpc.getSendEofMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSendEofMethod = RPIServiceGrpc.getSendEofMethod) == null) {
          RPIServiceGrpc.getSendEofMethod = getSendEofMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sendEof"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("sendEof"))
              .build();
        }
      }
    }
    return getSendEofMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getReplInterruptMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "replInterrupt",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getReplInterruptMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getReplInterruptMethod;
    if ((getReplInterruptMethod = RPIServiceGrpc.getReplInterruptMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getReplInterruptMethod = RPIServiceGrpc.getReplInterruptMethod) == null) {
          RPIServiceGrpc.getReplInterruptMethod = getReplInterruptMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "replInterrupt"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("replInterrupt"))
              .build();
        }
      }
    }
    return getReplInterruptMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.AsyncEvent> getGetAsyncEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAsyncEvents",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.AsyncEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.AsyncEvent> getGetAsyncEventsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.AsyncEvent> getGetAsyncEventsMethod;
    if ((getGetAsyncEventsMethod = RPIServiceGrpc.getGetAsyncEventsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetAsyncEventsMethod = RPIServiceGrpc.getGetAsyncEventsMethod) == null) {
          RPIServiceGrpc.getGetAsyncEventsMethod = getGetAsyncEventsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.AsyncEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAsyncEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.AsyncEvent.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getAsyncEvents"))
              .build();
        }
      }
    }
    return getGetAsyncEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest,
      com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugAddOrModifyBreakpoint",
      requestType = org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest,
      com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest, com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod;
    if ((getDebugAddOrModifyBreakpointMethod = RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugAddOrModifyBreakpointMethod = RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod) == null) {
          RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod = getDebugAddOrModifyBreakpointMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugAddOrModifyBreakpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugAddOrModifyBreakpoint"))
              .build();
        }
      }
    }
    return getDebugAddOrModifyBreakpointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest,
      com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugSetMasterBreakpoint",
      requestType = org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest,
      com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest, com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod;
    if ((getDebugSetMasterBreakpointMethod = RPIServiceGrpc.getDebugSetMasterBreakpointMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugSetMasterBreakpointMethod = RPIServiceGrpc.getDebugSetMasterBreakpointMethod) == null) {
          RPIServiceGrpc.getDebugSetMasterBreakpointMethod = getDebugSetMasterBreakpointMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugSetMasterBreakpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugSetMasterBreakpoint"))
              .build();
        }
      }
    }
    return getDebugSetMasterBreakpointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.google.protobuf.Empty> getDebugRemoveBreakpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugRemoveBreakpoint",
      requestType = com.google.protobuf.Int32Value.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.google.protobuf.Empty> getDebugRemoveBreakpointMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Int32Value, com.google.protobuf.Empty> getDebugRemoveBreakpointMethod;
    if ((getDebugRemoveBreakpointMethod = RPIServiceGrpc.getDebugRemoveBreakpointMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugRemoveBreakpointMethod = RPIServiceGrpc.getDebugRemoveBreakpointMethod) == null) {
          RPIServiceGrpc.getDebugRemoveBreakpointMethod = getDebugRemoveBreakpointMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Int32Value, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugRemoveBreakpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugRemoveBreakpoint"))
              .build();
        }
      }
    }
    return getDebugRemoveBreakpointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandContinueMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandContinue",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandContinueMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandContinueMethod;
    if ((getDebugCommandContinueMethod = RPIServiceGrpc.getDebugCommandContinueMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandContinueMethod = RPIServiceGrpc.getDebugCommandContinueMethod) == null) {
          RPIServiceGrpc.getDebugCommandContinueMethod = getDebugCommandContinueMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandContinue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandContinue"))
              .build();
        }
      }
    }
    return getDebugCommandContinueMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandPauseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandPause",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandPauseMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandPauseMethod;
    if ((getDebugCommandPauseMethod = RPIServiceGrpc.getDebugCommandPauseMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandPauseMethod = RPIServiceGrpc.getDebugCommandPauseMethod) == null) {
          RPIServiceGrpc.getDebugCommandPauseMethod = getDebugCommandPauseMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandPause"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandPause"))
              .build();
        }
      }
    }
    return getDebugCommandPauseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStopMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandStop",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStopMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandStopMethod;
    if ((getDebugCommandStopMethod = RPIServiceGrpc.getDebugCommandStopMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandStopMethod = RPIServiceGrpc.getDebugCommandStopMethod) == null) {
          RPIServiceGrpc.getDebugCommandStopMethod = getDebugCommandStopMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandStop"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandStop"))
              .build();
        }
      }
    }
    return getDebugCommandStopMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepOverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandStepOver",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepOverMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandStepOverMethod;
    if ((getDebugCommandStepOverMethod = RPIServiceGrpc.getDebugCommandStepOverMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandStepOverMethod = RPIServiceGrpc.getDebugCommandStepOverMethod) == null) {
          RPIServiceGrpc.getDebugCommandStepOverMethod = getDebugCommandStepOverMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandStepOver"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandStepOver"))
              .build();
        }
      }
    }
    return getDebugCommandStepOverMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepIntoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandStepInto",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepIntoMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandStepIntoMethod;
    if ((getDebugCommandStepIntoMethod = RPIServiceGrpc.getDebugCommandStepIntoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandStepIntoMethod = RPIServiceGrpc.getDebugCommandStepIntoMethod) == null) {
          RPIServiceGrpc.getDebugCommandStepIntoMethod = getDebugCommandStepIntoMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandStepInto"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandStepInto"))
              .build();
        }
      }
    }
    return getDebugCommandStepIntoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepIntoMyCodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandStepIntoMyCode",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepIntoMyCodeMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandStepIntoMyCodeMethod;
    if ((getDebugCommandStepIntoMyCodeMethod = RPIServiceGrpc.getDebugCommandStepIntoMyCodeMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandStepIntoMyCodeMethod = RPIServiceGrpc.getDebugCommandStepIntoMyCodeMethod) == null) {
          RPIServiceGrpc.getDebugCommandStepIntoMyCodeMethod = getDebugCommandStepIntoMyCodeMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandStepIntoMyCode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandStepIntoMyCode"))
              .build();
        }
      }
    }
    return getDebugCommandStepIntoMyCodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepOutMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandStepOut",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getDebugCommandStepOutMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getDebugCommandStepOutMethod;
    if ((getDebugCommandStepOutMethod = RPIServiceGrpc.getDebugCommandStepOutMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandStepOutMethod = RPIServiceGrpc.getDebugCommandStepOutMethod) == null) {
          RPIServiceGrpc.getDebugCommandStepOutMethod = getDebugCommandStepOutMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandStepOut"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandStepOut"))
              .build();
        }
      }
    }
    return getDebugCommandStepOutMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SourcePosition,
      com.google.protobuf.Empty> getDebugCommandRunToPositionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandRunToPosition",
      requestType = org.jetbrains.r.rinterop.SourcePosition.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SourcePosition,
      com.google.protobuf.Empty> getDebugCommandRunToPositionMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SourcePosition, com.google.protobuf.Empty> getDebugCommandRunToPositionMethod;
    if ((getDebugCommandRunToPositionMethod = RPIServiceGrpc.getDebugCommandRunToPositionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandRunToPositionMethod = RPIServiceGrpc.getDebugCommandRunToPositionMethod) == null) {
          RPIServiceGrpc.getDebugCommandRunToPositionMethod = getDebugCommandRunToPositionMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.SourcePosition, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandRunToPosition"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.SourcePosition.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugCommandRunToPosition"))
              .build();
        }
      }
    }
    return getDebugCommandRunToPositionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      com.google.protobuf.Empty> getDebugMuteBreakpointsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugMuteBreakpoints",
      requestType = com.google.protobuf.BoolValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      com.google.protobuf.Empty> getDebugMuteBreakpointsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.BoolValue, com.google.protobuf.Empty> getDebugMuteBreakpointsMethod;
    if ((getDebugMuteBreakpointsMethod = RPIServiceGrpc.getDebugMuteBreakpointsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugMuteBreakpointsMethod = RPIServiceGrpc.getDebugMuteBreakpointsMethod) == null) {
          RPIServiceGrpc.getDebugMuteBreakpointsMethod = getDebugMuteBreakpointsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.BoolValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugMuteBreakpoints"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugMuteBreakpoints"))
              .build();
        }
      }
    }
    return getDebugMuteBreakpointsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getFunctionSourcePosition",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod;
    if ((getGetFunctionSourcePositionMethod = RPIServiceGrpc.getGetFunctionSourcePositionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetFunctionSourcePositionMethod = RPIServiceGrpc.getGetFunctionSourcePositionMethod) == null) {
          RPIServiceGrpc.getGetFunctionSourcePositionMethod = getGetFunctionSourcePositionMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getFunctionSourcePosition"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getFunctionSourcePosition"))
              .build();
        }
      }
    }
    return getGetFunctionSourcePositionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.StringValue> getGetSourceFileTextMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getSourceFileText",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.StringValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.StringValue> getGetSourceFileTextMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.StringValue> getGetSourceFileTextMethod;
    if ((getGetSourceFileTextMethod = RPIServiceGrpc.getGetSourceFileTextMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetSourceFileTextMethod = RPIServiceGrpc.getGetSourceFileTextMethod) == null) {
          RPIServiceGrpc.getGetSourceFileTextMethod = getGetSourceFileTextMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.StringValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getSourceFileText"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getSourceFileText"))
              .build();
        }
      }
    }
    return getGetSourceFileTextMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.StringValue> getGetSourceFileNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getSourceFileName",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.StringValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.StringValue> getGetSourceFileNameMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.StringValue> getGetSourceFileNameMethod;
    if ((getGetSourceFileNameMethod = RPIServiceGrpc.getGetSourceFileNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetSourceFileNameMethod = RPIServiceGrpc.getGetSourceFileNameMethod) == null) {
          RPIServiceGrpc.getGetSourceFileNameMethod = getGetSourceFileNameMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.StringValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getSourceFileName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getSourceFileName"))
              .build();
        }
      }
    }
    return getGetSourceFileNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsInitRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsInitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsInit",
      requestType = org.jetbrains.r.rinterop.GraphicsInitRequest.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsInitRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsInitMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsInitRequest, org.jetbrains.r.rinterop.CommandOutput> getGraphicsInitMethod;
    if ((getGraphicsInitMethod = RPIServiceGrpc.getGraphicsInitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsInitMethod = RPIServiceGrpc.getGraphicsInitMethod) == null) {
          RPIServiceGrpc.getGraphicsInitMethod = getGraphicsInitMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GraphicsInitRequest, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsInit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsInitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsInit"))
              .build();
        }
      }
    }
    return getGraphicsInitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsDump",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.GraphicsDumpResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod;
    if ((getGraphicsDumpMethod = RPIServiceGrpc.getGraphicsDumpMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsDumpMethod = RPIServiceGrpc.getGraphicsDumpMethod) == null) {
          RPIServiceGrpc.getGraphicsDumpMethod = getGraphicsDumpMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.GraphicsDumpResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsDump"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsDumpResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsDump"))
              .build();
        }
      }
    }
    return getGraphicsDumpMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRescale",
      requestType = org.jetbrains.r.rinterop.GraphicsRescaleRequest.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleRequest, org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleMethod;
    if ((getGraphicsRescaleMethod = RPIServiceGrpc.getGraphicsRescaleMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRescaleMethod = RPIServiceGrpc.getGraphicsRescaleMethod) == null) {
          RPIServiceGrpc.getGraphicsRescaleMethod = getGraphicsRescaleMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GraphicsRescaleRequest, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRescale"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsRescaleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRescale"))
              .build();
        }
      }
    }
    return getGraphicsRescaleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleStoredMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRescaleStored",
      requestType = org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleStoredMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest, org.jetbrains.r.rinterop.CommandOutput> getGraphicsRescaleStoredMethod;
    if ((getGraphicsRescaleStoredMethod = RPIServiceGrpc.getGraphicsRescaleStoredMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRescaleStoredMethod = RPIServiceGrpc.getGraphicsRescaleStoredMethod) == null) {
          RPIServiceGrpc.getGraphicsRescaleStoredMethod = getGraphicsRescaleStoredMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRescaleStored"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRescaleStored"))
              .build();
        }
      }
    }
    return getGraphicsRescaleStoredMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ScreenParameters,
      com.google.protobuf.Empty> getGraphicsSetParametersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsSetParameters",
      requestType = org.jetbrains.r.rinterop.ScreenParameters.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ScreenParameters,
      com.google.protobuf.Empty> getGraphicsSetParametersMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ScreenParameters, com.google.protobuf.Empty> getGraphicsSetParametersMethod;
    if ((getGraphicsSetParametersMethod = RPIServiceGrpc.getGraphicsSetParametersMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsSetParametersMethod = RPIServiceGrpc.getGraphicsSetParametersMethod) == null) {
          RPIServiceGrpc.getGraphicsSetParametersMethod = getGraphicsSetParametersMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.ScreenParameters, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsSetParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ScreenParameters.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsSetParameters"))
              .build();
        }
      }
    }
    return getGraphicsSetParametersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest,
      org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsGetSnapshotPath",
      requestType = org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest.class,
      responseType = org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest,
      org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest, org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod;
    if ((getGraphicsGetSnapshotPathMethod = RPIServiceGrpc.getGraphicsGetSnapshotPathMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsGetSnapshotPathMethod = RPIServiceGrpc.getGraphicsGetSnapshotPathMethod) == null) {
          RPIServiceGrpc.getGraphicsGetSnapshotPathMethod = getGraphicsGetSnapshotPathMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest, org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsGetSnapshotPath"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsGetSnapshotPath"))
              .build();
        }
      }
    }
    return getGraphicsGetSnapshotPathMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsFetchPlot",
      requestType = com.google.protobuf.Int32Value.class,
      responseType = org.jetbrains.r.rinterop.GraphicsFetchPlotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Int32Value, org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod;
    if ((getGraphicsFetchPlotMethod = RPIServiceGrpc.getGraphicsFetchPlotMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsFetchPlotMethod = RPIServiceGrpc.getGraphicsFetchPlotMethod) == null) {
          RPIServiceGrpc.getGraphicsFetchPlotMethod = getGraphicsFetchPlotMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Int32Value, org.jetbrains.r.rinterop.GraphicsFetchPlotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsFetchPlot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GraphicsFetchPlotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsFetchPlot"))
              .build();
        }
      }
    }
    return getGraphicsFetchPlotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsCreateGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsCreateGroup",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsCreateGroupMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput> getGraphicsCreateGroupMethod;
    if ((getGraphicsCreateGroupMethod = RPIServiceGrpc.getGraphicsCreateGroupMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsCreateGroupMethod = RPIServiceGrpc.getGraphicsCreateGroupMethod) == null) {
          RPIServiceGrpc.getGraphicsCreateGroupMethod = getGraphicsCreateGroupMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsCreateGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsCreateGroup"))
              .build();
        }
      }
    }
    return getGraphicsCreateGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRemoveGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRemoveGroup",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsRemoveGroupMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput> getGraphicsRemoveGroupMethod;
    if ((getGraphicsRemoveGroupMethod = RPIServiceGrpc.getGraphicsRemoveGroupMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRemoveGroupMethod = RPIServiceGrpc.getGraphicsRemoveGroupMethod) == null) {
          RPIServiceGrpc.getGraphicsRemoveGroupMethod = getGraphicsRemoveGroupMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRemoveGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRemoveGroup"))
              .build();
        }
      }
    }
    return getGraphicsRemoveGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsShutdownMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsShutdown",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getGraphicsShutdownMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput> getGraphicsShutdownMethod;
    if ((getGraphicsShutdownMethod = RPIServiceGrpc.getGraphicsShutdownMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsShutdownMethod = RPIServiceGrpc.getGraphicsShutdownMethod) == null) {
          RPIServiceGrpc.getGraphicsShutdownMethod = getGraphicsShutdownMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsShutdown"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsShutdown"))
              .build();
        }
      }
    }
    return getGraphicsShutdownMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ChunkParameters,
      org.jetbrains.r.rinterop.CommandOutput> getBeforeChunkExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "beforeChunkExecution",
      requestType = org.jetbrains.r.rinterop.ChunkParameters.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ChunkParameters,
      org.jetbrains.r.rinterop.CommandOutput> getBeforeChunkExecutionMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ChunkParameters, org.jetbrains.r.rinterop.CommandOutput> getBeforeChunkExecutionMethod;
    if ((getBeforeChunkExecutionMethod = RPIServiceGrpc.getBeforeChunkExecutionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getBeforeChunkExecutionMethod = RPIServiceGrpc.getBeforeChunkExecutionMethod) == null) {
          RPIServiceGrpc.getBeforeChunkExecutionMethod = getBeforeChunkExecutionMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.ChunkParameters, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "beforeChunkExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ChunkParameters.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("beforeChunkExecution"))
              .build();
        }
      }
    }
    return getBeforeChunkExecutionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getAfterChunkExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "afterChunkExecution",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.CommandOutput> getAfterChunkExecutionMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput> getAfterChunkExecutionMethod;
    if ((getAfterChunkExecutionMethod = RPIServiceGrpc.getAfterChunkExecutionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getAfterChunkExecutionMethod = RPIServiceGrpc.getAfterChunkExecutionMethod) == null) {
          RPIServiceGrpc.getAfterChunkExecutionMethod = getAfterChunkExecutionMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "afterChunkExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("afterChunkExecution"))
              .build();
        }
      }
    }
    return getAfterChunkExecutionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getPullChunkOutputPathsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "pullChunkOutputPaths",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getPullChunkOutputPathsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList> getPullChunkOutputPathsMethod;
    if ((getPullChunkOutputPathsMethod = RPIServiceGrpc.getPullChunkOutputPathsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getPullChunkOutputPathsMethod = RPIServiceGrpc.getPullChunkOutputPathsMethod) == null) {
          RPIServiceGrpc.getPullChunkOutputPathsMethod = getPullChunkOutputPathsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "pullChunkOutputPaths"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("pullChunkOutputPaths"))
              .build();
        }
      }
    }
    return getPullChunkOutputPathsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoGetPackageVersionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoGetPackageVersion",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoGetPackageVersionMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput> getRepoGetPackageVersionMethod;
    if ((getRepoGetPackageVersionMethod = RPIServiceGrpc.getRepoGetPackageVersionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoGetPackageVersionMethod = RPIServiceGrpc.getRepoGetPackageVersionMethod) == null) {
          RPIServiceGrpc.getRepoGetPackageVersionMethod = getRepoGetPackageVersionMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoGetPackageVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoGetPackageVersion"))
              .build();
        }
      }
    }
    return getRepoGetPackageVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoInstallPackageRequest,
      com.google.protobuf.Empty> getRepoInstallPackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoInstallPackage",
      requestType = org.jetbrains.r.rinterop.RepoInstallPackageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoInstallPackageRequest,
      com.google.protobuf.Empty> getRepoInstallPackageMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoInstallPackageRequest, com.google.protobuf.Empty> getRepoInstallPackageMethod;
    if ((getRepoInstallPackageMethod = RPIServiceGrpc.getRepoInstallPackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoInstallPackageMethod = RPIServiceGrpc.getRepoInstallPackageMethod) == null) {
          RPIServiceGrpc.getRepoInstallPackageMethod = getRepoInstallPackageMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RepoInstallPackageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoInstallPackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RepoInstallPackageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoInstallPackage"))
              .build();
        }
      }
    }
    return getRepoInstallPackageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoAddLibraryPathMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoAddLibraryPath",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoAddLibraryPathMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput> getRepoAddLibraryPathMethod;
    if ((getRepoAddLibraryPathMethod = RPIServiceGrpc.getRepoAddLibraryPathMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoAddLibraryPathMethod = RPIServiceGrpc.getRepoAddLibraryPathMethod) == null) {
          RPIServiceGrpc.getRepoAddLibraryPathMethod = getRepoAddLibraryPathMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoAddLibraryPath"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoAddLibraryPath"))
              .build();
        }
      }
    }
    return getRepoAddLibraryPathMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoCheckPackageInstalled",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod;
    if ((getRepoCheckPackageInstalledMethod = RPIServiceGrpc.getRepoCheckPackageInstalledMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoCheckPackageInstalledMethod = RPIServiceGrpc.getRepoCheckPackageInstalledMethod) == null) {
          RPIServiceGrpc.getRepoCheckPackageInstalledMethod = getRepoCheckPackageInstalledMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoCheckPackageInstalled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoCheckPackageInstalled"))
              .build();
        }
      }
    }
    return getRepoCheckPackageInstalledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoRemovePackageRequest,
      com.google.protobuf.Empty> getRepoRemovePackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoRemovePackage",
      requestType = org.jetbrains.r.rinterop.RepoRemovePackageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoRemovePackageRequest,
      com.google.protobuf.Empty> getRepoRemovePackageMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RepoRemovePackageRequest, com.google.protobuf.Empty> getRepoRemovePackageMethod;
    if ((getRepoRemovePackageMethod = RPIServiceGrpc.getRepoRemovePackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoRemovePackageMethod = RPIServiceGrpc.getRepoRemovePackageMethod) == null) {
          RPIServiceGrpc.getRepoRemovePackageMethod = getRepoRemovePackageMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RepoRemovePackageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoRemovePackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RepoRemovePackageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoRemovePackage"))
              .build();
        }
      }
    }
    return getRepoRemovePackageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PreviewDataImportRequest,
      org.jetbrains.r.rinterop.CommandOutput> getPreviewDataImportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "previewDataImport",
      requestType = org.jetbrains.r.rinterop.PreviewDataImportRequest.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PreviewDataImportRequest,
      org.jetbrains.r.rinterop.CommandOutput> getPreviewDataImportMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PreviewDataImportRequest, org.jetbrains.r.rinterop.CommandOutput> getPreviewDataImportMethod;
    if ((getPreviewDataImportMethod = RPIServiceGrpc.getPreviewDataImportMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getPreviewDataImportMethod = RPIServiceGrpc.getPreviewDataImportMethod) == null) {
          RPIServiceGrpc.getPreviewDataImportMethod = getPreviewDataImportMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.PreviewDataImportRequest, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "previewDataImport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.PreviewDataImportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("previewDataImport"))
              .build();
        }
      }
    }
    return getPreviewDataImportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.CommitDataImportRequest,
      com.google.protobuf.Empty> getCommitDataImportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "commitDataImport",
      requestType = org.jetbrains.r.rinterop.CommitDataImportRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.CommitDataImportRequest,
      com.google.protobuf.Empty> getCommitDataImportMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.CommitDataImportRequest, com.google.protobuf.Empty> getCommitDataImportMethod;
    if ((getCommitDataImportMethod = RPIServiceGrpc.getCommitDataImportMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getCommitDataImportMethod = RPIServiceGrpc.getCommitDataImportMethod) == null) {
          RPIServiceGrpc.getCommitDataImportMethod = getCommitDataImportMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.CommitDataImportRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "commitDataImport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommitDataImportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("commitDataImport"))
              .build();
        }
      }
    }
    return getCommitDataImportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "copyToPersistentRef",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.CopyToPersistentRefResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod;
    if ((getCopyToPersistentRefMethod = RPIServiceGrpc.getCopyToPersistentRefMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getCopyToPersistentRefMethod = RPIServiceGrpc.getCopyToPersistentRefMethod) == null) {
          RPIServiceGrpc.getCopyToPersistentRefMethod = getCopyToPersistentRefMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.CopyToPersistentRefResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "copyToPersistentRef"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CopyToPersistentRefResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("copyToPersistentRef"))
              .build();
        }
      }
    }
    return getCopyToPersistentRefMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PersistentRefList,
      com.google.protobuf.Empty> getDisposePersistentRefsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "disposePersistentRefs",
      requestType = org.jetbrains.r.rinterop.PersistentRefList.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PersistentRefList,
      com.google.protobuf.Empty> getDisposePersistentRefsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.PersistentRefList, com.google.protobuf.Empty> getDisposePersistentRefsMethod;
    if ((getDisposePersistentRefsMethod = RPIServiceGrpc.getDisposePersistentRefsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDisposePersistentRefsMethod = RPIServiceGrpc.getDisposePersistentRefsMethod) == null) {
          RPIServiceGrpc.getDisposePersistentRefsMethod = getDisposePersistentRefsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.PersistentRefList, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "disposePersistentRefs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.PersistentRefList.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("disposePersistentRefs"))
              .build();
        }
      }
    }
    return getDisposePersistentRefsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetParentEnvs",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.ParentEnvsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod;
    if ((getLoaderGetParentEnvsMethod = RPIServiceGrpc.getLoaderGetParentEnvsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetParentEnvsMethod = RPIServiceGrpc.getLoaderGetParentEnvsMethod) == null) {
          RPIServiceGrpc.getLoaderGetParentEnvsMethod = getLoaderGetParentEnvsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ParentEnvsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetParentEnvs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ParentEnvsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetParentEnvs"))
              .build();
        }
      }
    }
    return getLoaderGetParentEnvsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetVariablesRequest,
      org.jetbrains.r.rinterop.VariablesResponse> getLoaderGetVariablesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetVariables",
      requestType = org.jetbrains.r.rinterop.GetVariablesRequest.class,
      responseType = org.jetbrains.r.rinterop.VariablesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetVariablesRequest,
      org.jetbrains.r.rinterop.VariablesResponse> getLoaderGetVariablesMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetVariablesRequest, org.jetbrains.r.rinterop.VariablesResponse> getLoaderGetVariablesMethod;
    if ((getLoaderGetVariablesMethod = RPIServiceGrpc.getLoaderGetVariablesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetVariablesMethod = RPIServiceGrpc.getLoaderGetVariablesMethod) == null) {
          RPIServiceGrpc.getLoaderGetVariablesMethod = getLoaderGetVariablesMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GetVariablesRequest, org.jetbrains.r.rinterop.VariablesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetVariables"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GetVariablesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.VariablesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetVariables"))
              .build();
        }
      }
    }
    return getLoaderGetVariablesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getLoaderGetLoadedNamespacesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetLoadedNamespaces",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getLoaderGetLoadedNamespacesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList> getLoaderGetLoadedNamespacesMethod;
    if ((getLoaderGetLoadedNamespacesMethod = RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetLoadedNamespacesMethod = RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod) == null) {
          RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod = getLoaderGetLoadedNamespacesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetLoadedNamespaces"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetLoadedNamespaces"))
              .build();
        }
      }
    }
    return getLoaderGetLoadedNamespacesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ValueInfo> getLoaderGetValueInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetValueInfo",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.ValueInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ValueInfo> getLoaderGetValueInfoMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ValueInfo> getLoaderGetValueInfoMethod;
    if ((getLoaderGetValueInfoMethod = RPIServiceGrpc.getLoaderGetValueInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetValueInfoMethod = RPIServiceGrpc.getLoaderGetValueInfoMethod) == null) {
          RPIServiceGrpc.getLoaderGetValueInfoMethod = getLoaderGetValueInfoMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ValueInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetValueInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ValueInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetValueInfo"))
              .build();
        }
      }
    }
    return getLoaderGetValueInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringOrError> getEvaluateAsTextMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "evaluateAsText",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.StringOrError.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringOrError> getEvaluateAsTextMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringOrError> getEvaluateAsTextMethod;
    if ((getEvaluateAsTextMethod = RPIServiceGrpc.getEvaluateAsTextMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getEvaluateAsTextMethod = RPIServiceGrpc.getEvaluateAsTextMethod) == null) {
          RPIServiceGrpc.getEvaluateAsTextMethod = getEvaluateAsTextMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringOrError>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "evaluateAsText"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringOrError.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("evaluateAsText"))
              .build();
        }
      }
    }
    return getEvaluateAsTextMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "evaluateAsBoolean",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = com.google.protobuf.BoolValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod;
    if ((getEvaluateAsBooleanMethod = RPIServiceGrpc.getEvaluateAsBooleanMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getEvaluateAsBooleanMethod = RPIServiceGrpc.getEvaluateAsBooleanMethod) == null) {
          RPIServiceGrpc.getEvaluateAsBooleanMethod = getEvaluateAsBooleanMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, com.google.protobuf.BoolValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "evaluateAsBoolean"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("evaluateAsBoolean"))
              .build();
        }
      }
    }
    return getEvaluateAsBooleanMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getGetDistinctStringsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDistinctStrings",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getGetDistinctStringsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList> getGetDistinctStringsMethod;
    if ((getGetDistinctStringsMethod = RPIServiceGrpc.getGetDistinctStringsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDistinctStringsMethod = RPIServiceGrpc.getGetDistinctStringsMethod) == null) {
          RPIServiceGrpc.getGetDistinctStringsMethod = getGetDistinctStringsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDistinctStrings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getDistinctStrings"))
              .build();
        }
      }
    }
    return getGetDistinctStringsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getLoadObjectNamesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadObjectNames",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getLoadObjectNamesMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList> getLoadObjectNamesMethod;
    if ((getLoadObjectNamesMethod = RPIServiceGrpc.getLoadObjectNamesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadObjectNamesMethod = RPIServiceGrpc.getLoadObjectNamesMethod) == null) {
          RPIServiceGrpc.getLoadObjectNamesMethod = getLoadObjectNamesMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadObjectNames"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadObjectNames"))
              .build();
        }
      }
    }
    return getLoadObjectNamesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getFindInheritorNamedArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "findInheritorNamedArguments",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getFindInheritorNamedArgumentsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList> getFindInheritorNamedArgumentsMethod;
    if ((getFindInheritorNamedArgumentsMethod = RPIServiceGrpc.getFindInheritorNamedArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getFindInheritorNamedArgumentsMethod = RPIServiceGrpc.getFindInheritorNamedArgumentsMethod) == null) {
          RPIServiceGrpc.getFindInheritorNamedArgumentsMethod = getFindInheritorNamedArgumentsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "findInheritorNamedArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("findInheritorNamedArguments"))
              .build();
        }
      }
    }
    return getFindInheritorNamedArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "findExtraNamedArguments",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.ExtraNamedArguments.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod;
    if ((getFindExtraNamedArgumentsMethod = RPIServiceGrpc.getFindExtraNamedArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getFindExtraNamedArgumentsMethod = RPIServiceGrpc.getFindExtraNamedArgumentsMethod) == null) {
          RPIServiceGrpc.getFindExtraNamedArgumentsMethod = getFindExtraNamedArgumentsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.ExtraNamedArguments>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "findExtraNamedArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ExtraNamedArguments.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("findExtraNamedArguments"))
              .build();
        }
      }
    }
    return getFindExtraNamedArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getS4ClassInfoByObjectName",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.classes.S4ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod;
    if ((getGetS4ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetS4ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod) == null) {
          RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod = getGetS4ClassInfoByObjectNameMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.classes.S4ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getS4ClassInfoByObjectName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.S4ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getS4ClassInfoByObjectName"))
              .build();
        }
      }
    }
    return getGetS4ClassInfoByObjectNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getR6ClassInfoByObjectName",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.classes.R6ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod;
    if ((getGetR6ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetR6ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod) == null) {
          RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod = getGetR6ClassInfoByObjectNameMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.classes.R6ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getR6ClassInfoByObjectName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.R6ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getR6ClassInfoByObjectName"))
              .build();
        }
      }
    }
    return getGetR6ClassInfoByObjectNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.TableColumnsInfoRequest,
      org.jetbrains.r.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getTableColumnsInfo",
      requestType = org.jetbrains.r.rinterop.TableColumnsInfoRequest.class,
      responseType = org.jetbrains.r.rinterop.TableColumnsInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.TableColumnsInfoRequest,
      org.jetbrains.r.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.TableColumnsInfoRequest, org.jetbrains.r.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod;
    if ((getGetTableColumnsInfoMethod = RPIServiceGrpc.getGetTableColumnsInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetTableColumnsInfoMethod = RPIServiceGrpc.getGetTableColumnsInfoMethod) == null) {
          RPIServiceGrpc.getGetTableColumnsInfoMethod = getGetTableColumnsInfoMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.TableColumnsInfoRequest, org.jetbrains.r.rinterop.TableColumnsInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getTableColumnsInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.TableColumnsInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.TableColumnsInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getTableColumnsInfo"))
              .build();
        }
      }
    }
    return getGetTableColumnsInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getGetFormalArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getFormalArguments",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.StringList> getGetFormalArgumentsMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList> getGetFormalArgumentsMethod;
    if ((getGetFormalArgumentsMethod = RPIServiceGrpc.getGetFormalArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetFormalArgumentsMethod = RPIServiceGrpc.getGetFormalArgumentsMethod) == null) {
          RPIServiceGrpc.getGetFormalArgumentsMethod = getGetFormalArgumentsMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getFormalArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getFormalArguments"))
              .build();
        }
      }
    }
    return getGetFormalArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Int64Value> getGetEqualityObjectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEqualityObject",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = com.google.protobuf.Int64Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Int64Value> getGetEqualityObjectMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Int64Value> getGetEqualityObjectMethod;
    if ((getGetEqualityObjectMethod = RPIServiceGrpc.getGetEqualityObjectMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetEqualityObjectMethod = RPIServiceGrpc.getGetEqualityObjectMethod) == null) {
          RPIServiceGrpc.getGetEqualityObjectMethod = getGetEqualityObjectMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Int64Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEqualityObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int64Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getEqualityObject"))
              .build();
        }
      }
    }
    return getGetEqualityObjectMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SetValueRequest,
      org.jetbrains.r.rinterop.ValueInfo> getSetValueMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setValue",
      requestType = org.jetbrains.r.rinterop.SetValueRequest.class,
      responseType = org.jetbrains.r.rinterop.ValueInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SetValueRequest,
      org.jetbrains.r.rinterop.ValueInfo> getSetValueMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.SetValueRequest, org.jetbrains.r.rinterop.ValueInfo> getSetValueMethod;
    if ((getSetValueMethod = RPIServiceGrpc.getSetValueMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetValueMethod = RPIServiceGrpc.getSetValueMethod) == null) {
          RPIServiceGrpc.getSetValueMethod = getSetValueMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.SetValueRequest, org.jetbrains.r.rinterop.ValueInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setValue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.SetValueRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ValueInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setValue"))
              .build();
        }
      }
    }
    return getSetValueMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRefList,
      org.jetbrains.r.rinterop.Int64List> getGetObjectSizesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getObjectSizes",
      requestType = org.jetbrains.r.rinterop.RRefList.class,
      responseType = org.jetbrains.r.rinterop.Int64List.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRefList,
      org.jetbrains.r.rinterop.Int64List> getGetObjectSizesMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRefList, org.jetbrains.r.rinterop.Int64List> getGetObjectSizesMethod;
    if ((getGetObjectSizesMethod = RPIServiceGrpc.getGetObjectSizesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetObjectSizesMethod = RPIServiceGrpc.getGetObjectSizesMethod) == null) {
          RPIServiceGrpc.getGetObjectSizesMethod = getGetObjectSizesMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRefList, org.jetbrains.r.rinterop.Int64List>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getObjectSizes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRefList.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.Int64List.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getObjectSizes"))
              .build();
        }
      }
    }
    return getGetObjectSizesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getGetRMarkdownChunkOptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getRMarkdownChunkOptions",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.StringList> getGetRMarkdownChunkOptionsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList> getGetRMarkdownChunkOptionsMethod;
    if ((getGetRMarkdownChunkOptionsMethod = RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetRMarkdownChunkOptionsMethod = RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod) == null) {
          RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod = getGetRMarkdownChunkOptionsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getRMarkdownChunkOptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getRMarkdownChunkOptions"))
              .build();
        }
      }
    }
    return getGetRMarkdownChunkOptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Int32Value> getDataFrameRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameRegister",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Int32Value> getDataFrameRegisterMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Int32Value> getDataFrameRegisterMethod;
    if ((getDataFrameRegisterMethod = RPIServiceGrpc.getDataFrameRegisterMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameRegisterMethod = RPIServiceGrpc.getDataFrameRegisterMethod) == null) {
          RPIServiceGrpc.getDataFrameRegisterMethod = getDataFrameRegisterMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameRegister"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameRegister"))
              .build();
        }
      }
    }
    return getDataFrameRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameGetInfo",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = org.jetbrains.r.rinterop.DataFrameInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      org.jetbrains.r.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod;
    if ((getDataFrameGetInfoMethod = RPIServiceGrpc.getDataFrameGetInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameGetInfoMethod = RPIServiceGrpc.getDataFrameGetInfoMethod) == null) {
          RPIServiceGrpc.getDataFrameGetInfoMethod = getDataFrameGetInfoMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, org.jetbrains.r.rinterop.DataFrameInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameGetInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DataFrameInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameGetInfo"))
              .build();
        }
      }
    }
    return getDataFrameGetInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameGetDataRequest,
      org.jetbrains.r.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameGetData",
      requestType = org.jetbrains.r.rinterop.DataFrameGetDataRequest.class,
      responseType = org.jetbrains.r.rinterop.DataFrameGetDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameGetDataRequest,
      org.jetbrains.r.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameGetDataRequest, org.jetbrains.r.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod;
    if ((getDataFrameGetDataMethod = RPIServiceGrpc.getDataFrameGetDataMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameGetDataMethod = RPIServiceGrpc.getDataFrameGetDataMethod) == null) {
          RPIServiceGrpc.getDataFrameGetDataMethod = getDataFrameGetDataMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DataFrameGetDataRequest, org.jetbrains.r.rinterop.DataFrameGetDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameGetData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DataFrameGetDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DataFrameGetDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameGetData"))
              .build();
        }
      }
    }
    return getDataFrameGetDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameSortRequest,
      com.google.protobuf.Int32Value> getDataFrameSortMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameSort",
      requestType = org.jetbrains.r.rinterop.DataFrameSortRequest.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameSortRequest,
      com.google.protobuf.Int32Value> getDataFrameSortMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameSortRequest, com.google.protobuf.Int32Value> getDataFrameSortMethod;
    if ((getDataFrameSortMethod = RPIServiceGrpc.getDataFrameSortMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameSortMethod = RPIServiceGrpc.getDataFrameSortMethod) == null) {
          RPIServiceGrpc.getDataFrameSortMethod = getDataFrameSortMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DataFrameSortRequest, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameSort"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DataFrameSortRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameSort"))
              .build();
        }
      }
    }
    return getDataFrameSortMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameFilterRequest,
      com.google.protobuf.Int32Value> getDataFrameFilterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameFilter",
      requestType = org.jetbrains.r.rinterop.DataFrameFilterRequest.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameFilterRequest,
      com.google.protobuf.Int32Value> getDataFrameFilterMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DataFrameFilterRequest, com.google.protobuf.Int32Value> getDataFrameFilterMethod;
    if ((getDataFrameFilterMethod = RPIServiceGrpc.getDataFrameFilterMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameFilterMethod = RPIServiceGrpc.getDataFrameFilterMethod) == null) {
          RPIServiceGrpc.getDataFrameFilterMethod = getDataFrameFilterMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DataFrameFilterRequest, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameFilter"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DataFrameFilterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameFilter"))
              .build();
        }
      }
    }
    return getDataFrameFilterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.BoolValue> getDataFrameRefreshMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameRefresh",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = com.google.protobuf.BoolValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.BoolValue> getDataFrameRefreshMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, com.google.protobuf.BoolValue> getDataFrameRefreshMethod;
    if ((getDataFrameRefreshMethod = RPIServiceGrpc.getDataFrameRefreshMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameRefreshMethod = RPIServiceGrpc.getDataFrameRefreshMethod) == null) {
          RPIServiceGrpc.getDataFrameRefreshMethod = getDataFrameRefreshMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, com.google.protobuf.BoolValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameRefresh"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameRefresh"))
              .build();
        }
      }
    }
    return getDataFrameRefreshMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest,
      org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "convertRoxygenToHTML",
      requestType = org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest.class,
      responseType = org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest,
      org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest, org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod;
    if ((getConvertRoxygenToHTMLMethod = RPIServiceGrpc.getConvertRoxygenToHTMLMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getConvertRoxygenToHTMLMethod = RPIServiceGrpc.getConvertRoxygenToHTMLMethod) == null) {
          RPIServiceGrpc.getConvertRoxygenToHTMLMethod = getConvertRoxygenToHTMLMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest, org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "convertRoxygenToHTML"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("convertRoxygenToHTML"))
              .build();
        }
      }
    }
    return getConvertRoxygenToHTMLMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.HttpdResponse> getHttpdRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "httpdRequest",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.HttpdResponse> getHttpdRequestMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.HttpdResponse> getHttpdRequestMethod;
    if ((getHttpdRequestMethod = RPIServiceGrpc.getHttpdRequestMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getHttpdRequestMethod = RPIServiceGrpc.getHttpdRequestMethod) == null) {
          RPIServiceGrpc.getHttpdRequestMethod = getHttpdRequestMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "httpdRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.HttpdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("httpdRequest"))
              .build();
        }
      }
    }
    return getHttpdRequestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForPackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDocumentationForPackage",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForPackageMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForPackageMethod;
    if ((getGetDocumentationForPackageMethod = RPIServiceGrpc.getGetDocumentationForPackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDocumentationForPackageMethod = RPIServiceGrpc.getGetDocumentationForPackageMethod) == null) {
          RPIServiceGrpc.getGetDocumentationForPackageMethod = getGetDocumentationForPackageMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDocumentationForPackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.HttpdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getDocumentationForPackage"))
              .build();
        }
      }
    }
    return getGetDocumentationForPackageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DocumentationForSymbolRequest,
      org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDocumentationForSymbol",
      requestType = org.jetbrains.r.rinterop.DocumentationForSymbolRequest.class,
      responseType = org.jetbrains.r.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DocumentationForSymbolRequest,
      org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.DocumentationForSymbolRequest, org.jetbrains.r.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod;
    if ((getGetDocumentationForSymbolMethod = RPIServiceGrpc.getGetDocumentationForSymbolMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDocumentationForSymbolMethod = RPIServiceGrpc.getGetDocumentationForSymbolMethod) == null) {
          RPIServiceGrpc.getGetDocumentationForSymbolMethod = getGetDocumentationForSymbolMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.DocumentationForSymbolRequest, org.jetbrains.r.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDocumentationForSymbol"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.DocumentationForSymbolRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.HttpdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getDocumentationForSymbol"))
              .build();
        }
      }
    }
    return getGetDocumentationForSymbolMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Int32Value> getStartHttpdMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "startHttpd",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Int32Value> getStartHttpdMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Int32Value> getStartHttpdMethod;
    if ((getStartHttpdMethod = RPIServiceGrpc.getStartHttpdMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getStartHttpdMethod = RPIServiceGrpc.getStartHttpdMethod) == null) {
          RPIServiceGrpc.getStartHttpdMethod = getStartHttpdMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "startHttpd"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("startHttpd"))
              .build();
        }
      }
    }
    return getStartHttpdMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.StringValue> getGetWorkingDirMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getWorkingDir",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.StringValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.StringValue> getGetWorkingDirMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.StringValue> getGetWorkingDirMethod;
    if ((getGetWorkingDirMethod = RPIServiceGrpc.getGetWorkingDirMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetWorkingDirMethod = RPIServiceGrpc.getGetWorkingDirMethod) == null) {
          RPIServiceGrpc.getGetWorkingDirMethod = getGetWorkingDirMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.StringValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getWorkingDir"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getWorkingDir"))
              .build();
        }
      }
    }
    return getGetWorkingDirMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSetWorkingDirMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setWorkingDir",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSetWorkingDirMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.Empty> getSetWorkingDirMethod;
    if ((getSetWorkingDirMethod = RPIServiceGrpc.getSetWorkingDirMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetWorkingDirMethod = RPIServiceGrpc.getSetWorkingDirMethod) == null) {
          RPIServiceGrpc.getSetWorkingDirMethod = getSetWorkingDirMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setWorkingDir"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setWorkingDir"))
              .build();
        }
      }
    }
    return getSetWorkingDirMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Empty> getClearEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "clearEnvironment",
      requestType = org.jetbrains.r.rinterop.RRef.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef,
      com.google.protobuf.Empty> getClearEnvironmentMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Empty> getClearEnvironmentMethod;
    if ((getClearEnvironmentMethod = RPIServiceGrpc.getClearEnvironmentMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getClearEnvironmentMethod = RPIServiceGrpc.getClearEnvironmentMethod) == null) {
          RPIServiceGrpc.getClearEnvironmentMethod = getClearEnvironmentMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RRef, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "clearEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("clearEnvironment"))
              .build();
        }
      }
    }
    return getClearEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetSysEnvRequest,
      org.jetbrains.r.rinterop.StringList> getGetSysEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getSysEnv",
      requestType = org.jetbrains.r.rinterop.GetSysEnvRequest.class,
      responseType = org.jetbrains.r.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetSysEnvRequest,
      org.jetbrains.r.rinterop.StringList> getGetSysEnvMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.GetSysEnvRequest, org.jetbrains.r.rinterop.StringList> getGetSysEnvMethod;
    if ((getGetSysEnvMethod = RPIServiceGrpc.getGetSysEnvMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetSysEnvMethod = RPIServiceGrpc.getGetSysEnvMethod) == null) {
          RPIServiceGrpc.getGetSysEnvMethod = getGetSysEnvMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.GetSysEnvRequest, org.jetbrains.r.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getSysEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.GetSysEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getSysEnv"))
              .build();
        }
      }
    }
    return getGetSysEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadInstalledPackages",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.RInstalledPackageList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod;
    if ((getLoadInstalledPackagesMethod = RPIServiceGrpc.getLoadInstalledPackagesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadInstalledPackagesMethod = RPIServiceGrpc.getLoadInstalledPackagesMethod) == null) {
          RPIServiceGrpc.getLoadInstalledPackagesMethod = getLoadInstalledPackagesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.RInstalledPackageList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadInstalledPackages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RInstalledPackageList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadInstalledPackages"))
              .build();
        }
      }
    }
    return getLoadInstalledPackagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.RLibraryPathList> getLoadLibPathsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadLibPaths",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.rinterop.RLibraryPathList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.rinterop.RLibraryPathList> getLoadLibPathsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.rinterop.RLibraryPathList> getLoadLibPathsMethod;
    if ((getLoadLibPathsMethod = RPIServiceGrpc.getLoadLibPathsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadLibPathsMethod = RPIServiceGrpc.getLoadLibPathsMethod) == null) {
          RPIServiceGrpc.getLoadLibPathsMethod = getLoadLibPathsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.rinterop.RLibraryPathList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadLibPaths"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RLibraryPathList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadLibPaths"))
              .build();
        }
      }
    }
    return getLoadLibPathsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getLoadLibraryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadLibrary",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getLoadLibraryMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.Empty> getLoadLibraryMethod;
    if ((getLoadLibraryMethod = RPIServiceGrpc.getLoadLibraryMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadLibraryMethod = RPIServiceGrpc.getLoadLibraryMethod) == null) {
          RPIServiceGrpc.getLoadLibraryMethod = getLoadLibraryMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadLibrary"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadLibrary"))
              .build();
        }
      }
    }
    return getLoadLibraryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.UnloadLibraryRequest,
      com.google.protobuf.Empty> getUnloadLibraryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "unloadLibrary",
      requestType = org.jetbrains.r.rinterop.UnloadLibraryRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.UnloadLibraryRequest,
      com.google.protobuf.Empty> getUnloadLibraryMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.UnloadLibraryRequest, com.google.protobuf.Empty> getUnloadLibraryMethod;
    if ((getUnloadLibraryMethod = RPIServiceGrpc.getUnloadLibraryMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getUnloadLibraryMethod = RPIServiceGrpc.getUnloadLibraryMethod) == null) {
          RPIServiceGrpc.getUnloadLibraryMethod = getUnloadLibraryMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.UnloadLibraryRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "unloadLibrary"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.UnloadLibraryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("unloadLibrary"))
              .build();
        }
      }
    }
    return getUnloadLibraryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSaveGlobalEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "saveGlobalEnvironment",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.google.protobuf.Empty> getSaveGlobalEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.google.protobuf.Empty> getSaveGlobalEnvironmentMethod;
    if ((getSaveGlobalEnvironmentMethod = RPIServiceGrpc.getSaveGlobalEnvironmentMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSaveGlobalEnvironmentMethod = RPIServiceGrpc.getSaveGlobalEnvironmentMethod) == null) {
          RPIServiceGrpc.getSaveGlobalEnvironmentMethod = getSaveGlobalEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "saveGlobalEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("saveGlobalEnvironment"))
              .build();
        }
      }
    }
    return getSaveGlobalEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.LoadEnvironmentRequest,
      com.google.protobuf.Empty> getLoadEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadEnvironment",
      requestType = org.jetbrains.r.rinterop.LoadEnvironmentRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.LoadEnvironmentRequest,
      com.google.protobuf.Empty> getLoadEnvironmentMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.LoadEnvironmentRequest, com.google.protobuf.Empty> getLoadEnvironmentMethod;
    if ((getLoadEnvironmentMethod = RPIServiceGrpc.getLoadEnvironmentMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadEnvironmentMethod = RPIServiceGrpc.getLoadEnvironmentMethod) == null) {
          RPIServiceGrpc.getLoadEnvironmentMethod = getLoadEnvironmentMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.LoadEnvironmentRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.LoadEnvironmentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadEnvironment"))
              .build();
        }
      }
    }
    return getLoadEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.google.protobuf.Empty> getSetOutputWidthMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setOutputWidth",
      requestType = com.google.protobuf.Int32Value.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.google.protobuf.Empty> getSetOutputWidthMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Int32Value, com.google.protobuf.Empty> getSetOutputWidthMethod;
    if ((getSetOutputWidthMethod = RPIServiceGrpc.getSetOutputWidthMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetOutputWidthMethod = RPIServiceGrpc.getSetOutputWidthMethod) == null) {
          RPIServiceGrpc.getSetOutputWidthMethod = getSetOutputWidthMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Int32Value, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setOutputWidth"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setOutputWidth"))
              .build();
        }
      }
    }
    return getSetOutputWidthMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getClientRequestFinishedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "clientRequestFinished",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getClientRequestFinishedMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getClientRequestFinishedMethod;
    if ((getClientRequestFinishedMethod = RPIServiceGrpc.getClientRequestFinishedMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getClientRequestFinishedMethod = RPIServiceGrpc.getClientRequestFinishedMethod) == null) {
          RPIServiceGrpc.getClientRequestFinishedMethod = getClientRequestFinishedMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "clientRequestFinished"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("clientRequestFinished"))
              .build();
        }
      }
    }
    return getClientRequestFinishedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RObject,
      com.google.protobuf.Empty> getRStudioApiResponseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "rStudioApiResponse",
      requestType = org.jetbrains.r.rinterop.RObject.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RObject,
      com.google.protobuf.Empty> getRStudioApiResponseMethod() {
    io.grpc.MethodDescriptor<org.jetbrains.r.rinterop.RObject, com.google.protobuf.Empty> getRStudioApiResponseMethod;
    if ((getRStudioApiResponseMethod = RPIServiceGrpc.getRStudioApiResponseMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRStudioApiResponseMethod = RPIServiceGrpc.getRStudioApiResponseMethod) == null) {
          RPIServiceGrpc.getRStudioApiResponseMethod = getRStudioApiResponseMethod =
              io.grpc.MethodDescriptor.<org.jetbrains.r.rinterop.RObject, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "rStudioApiResponse"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.RObject.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("rStudioApiResponse"))
              .build();
        }
      }
    }
    return getRStudioApiResponseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      com.google.protobuf.Empty> getSetSaveOnExitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setSaveOnExit",
      requestType = com.google.protobuf.BoolValue.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      com.google.protobuf.Empty> getSetSaveOnExitMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.BoolValue, com.google.protobuf.Empty> getSetSaveOnExitMethod;
    if ((getSetSaveOnExitMethod = RPIServiceGrpc.getSetSaveOnExitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetSaveOnExitMethod = RPIServiceGrpc.getSetSaveOnExitMethod) == null) {
          RPIServiceGrpc.getSetSaveOnExitMethod = getSetSaveOnExitMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.BoolValue, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setSaveOnExit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setSaveOnExit"))
              .build();
        }
      }
    }
    return getSetSaveOnExitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      org.jetbrains.r.rinterop.CommandOutput> getSetRStudioApiEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setRStudioApiEnabled",
      requestType = com.google.protobuf.BoolValue.class,
      responseType = org.jetbrains.r.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      org.jetbrains.r.rinterop.CommandOutput> getSetRStudioApiEnabledMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.BoolValue, org.jetbrains.r.rinterop.CommandOutput> getSetRStudioApiEnabledMethod;
    if ((getSetRStudioApiEnabledMethod = RPIServiceGrpc.getSetRStudioApiEnabledMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetRStudioApiEnabledMethod = RPIServiceGrpc.getSetRStudioApiEnabledMethod) == null) {
          RPIServiceGrpc.getSetRStudioApiEnabledMethod = getSetRStudioApiEnabledMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.BoolValue, org.jetbrains.r.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setRStudioApiEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setRStudioApiEnabled"))
              .build();
        }
      }
    }
    return getSetRStudioApiEnabledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLoadedShortS4ClassInfos",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.classes.ShortS4ClassInfoList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod;
    if ((getGetLoadedShortS4ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetLoadedShortS4ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod) == null) {
          RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod = getGetLoadedShortS4ClassInfosMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.classes.ShortS4ClassInfoList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLoadedShortS4ClassInfos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.ShortS4ClassInfoList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getLoadedShortS4ClassInfos"))
              .build();
        }
      }
    }
    return getGetLoadedShortS4ClassInfosMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getS4ClassInfoByClassName",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.classes.S4ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod;
    if ((getGetS4ClassInfoByClassNameMethod = RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetS4ClassInfoByClassNameMethod = RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod) == null) {
          RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod = getGetS4ClassInfoByClassNameMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.classes.S4ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getS4ClassInfoByClassName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.S4ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getS4ClassInfoByClassName"))
              .build();
        }
      }
    }
    return getGetS4ClassInfoByClassNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLoadedShortR6ClassInfos",
      requestType = com.google.protobuf.Empty.class,
      responseType = org.jetbrains.r.classes.ShortR6ClassInfoList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      org.jetbrains.r.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, org.jetbrains.r.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod;
    if ((getGetLoadedShortR6ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetLoadedShortR6ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod) == null) {
          RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod = getGetLoadedShortR6ClassInfosMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, org.jetbrains.r.classes.ShortR6ClassInfoList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLoadedShortR6ClassInfos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.ShortR6ClassInfoList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getLoadedShortR6ClassInfos"))
              .build();
        }
      }
    }
    return getGetLoadedShortR6ClassInfosMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getR6ClassInfoByClassName",
      requestType = com.google.protobuf.StringValue.class,
      responseType = org.jetbrains.r.classes.R6ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, org.jetbrains.r.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod;
    if ((getGetR6ClassInfoByClassNameMethod = RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetR6ClassInfoByClassNameMethod = RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod) == null) {
          RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod = getGetR6ClassInfoByClassNameMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, org.jetbrains.r.classes.R6ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getR6ClassInfoByClassName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.jetbrains.r.classes.R6ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getR6ClassInfoByClassName"))
              .build();
        }
      }
    }
    return getGetR6ClassInfoByClassNameMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RPIServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RPIServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RPIServiceStub>() {
        @java.lang.Override
        public RPIServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RPIServiceStub(channel, callOptions);
        }
      };
    return RPIServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RPIServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RPIServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RPIServiceBlockingStub>() {
        @java.lang.Override
        public RPIServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RPIServiceBlockingStub(channel, callOptions);
        }
      };
    return RPIServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RPIServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RPIServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RPIServiceFutureStub>() {
        @java.lang.Override
        public RPIServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RPIServiceFutureStub(channel, callOptions);
        }
      };
    return RPIServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class RPIServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void getInfo(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetInfoMethod(), responseObserver);
    }

    /**
     */
    public void isBusy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnimplementedUnaryCall(getIsBusyMethod(), responseObserver);
    }

    /**
     */
    public void init(org.jetbrains.r.rinterop.Init request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getInitMethod(), responseObserver);
    }

    /**
     */
    public void quit(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getQuitMethod(), responseObserver);
    }

    /**
     */
    public void quitProceed(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getQuitProceedMethod(), responseObserver);
    }

    /**
     */
    public void executeCode(org.jetbrains.r.rinterop.ExecuteCodeRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExecuteCodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteCodeMethod(), responseObserver);
    }

    /**
     */
    public void sendReadLn(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSendReadLnMethod(), responseObserver);
    }

    /**
     */
    public void sendEof(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSendEofMethod(), responseObserver);
    }

    /**
     */
    public void replInterrupt(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getReplInterruptMethod(), responseObserver);
    }

    /**
     */
    public void getAsyncEvents(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.AsyncEvent> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAsyncEventsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public void debugAddOrModifyBreakpoint(org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugAddOrModifyBreakpointMethod(), responseObserver);
    }

    /**
     */
    public void debugSetMasterBreakpoint(org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugSetMasterBreakpointMethod(), responseObserver);
    }

    /**
     */
    public void debugRemoveBreakpoint(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugRemoveBreakpointMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandContinue(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandContinueMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandPause(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandPauseMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandStop(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandStopMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandStepOver(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandStepOverMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandStepInto(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandStepIntoMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandStepIntoMyCode(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandStepIntoMyCodeMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandStepOut(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandStepOutMethod(), responseObserver);
    }

    /**
     */
    public void debugCommandRunToPosition(org.jetbrains.r.rinterop.SourcePosition request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugCommandRunToPositionMethod(), responseObserver);
    }

    /**
     */
    public void debugMuteBreakpoints(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDebugMuteBreakpointsMethod(), responseObserver);
    }

    /**
     */
    public void getFunctionSourcePosition(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetFunctionSourcePositionMethod(), responseObserver);
    }

    /**
     */
    public void getSourceFileText(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnimplementedUnaryCall(getGetSourceFileTextMethod(), responseObserver);
    }

    /**
     */
    public void getSourceFileName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnimplementedUnaryCall(getGetSourceFileNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    public void graphicsInit(org.jetbrains.r.rinterop.GraphicsInitRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsInitMethod(), responseObserver);
    }

    /**
     */
    public void graphicsDump(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsDumpResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsDumpMethod(), responseObserver);
    }

    /**
     */
    public void graphicsRescale(org.jetbrains.r.rinterop.GraphicsRescaleRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsRescaleMethod(), responseObserver);
    }

    /**
     */
    public void graphicsRescaleStored(org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsRescaleStoredMethod(), responseObserver);
    }

    /**
     */
    public void graphicsSetParameters(org.jetbrains.r.rinterop.ScreenParameters request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsSetParametersMethod(), responseObserver);
    }

    /**
     */
    public void graphicsGetSnapshotPath(org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsGetSnapshotPathMethod(), responseObserver);
    }

    /**
     */
    public void graphicsFetchPlot(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsFetchPlotMethod(), responseObserver);
    }

    /**
     */
    public void graphicsCreateGroup(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsCreateGroupMethod(), responseObserver);
    }

    /**
     */
    public void graphicsRemoveGroup(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsRemoveGroupMethod(), responseObserver);
    }

    /**
     */
    public void graphicsShutdown(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getGraphicsShutdownMethod(), responseObserver);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    public void beforeChunkExecution(org.jetbrains.r.rinterop.ChunkParameters request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getBeforeChunkExecutionMethod(), responseObserver);
    }

    /**
     */
    public void afterChunkExecution(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getAfterChunkExecutionMethod(), responseObserver);
    }

    /**
     */
    public void pullChunkOutputPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getPullChunkOutputPathsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    public void repoGetPackageVersion(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getRepoGetPackageVersionMethod(), responseObserver);
    }

    /**
     */
    public void repoInstallPackage(org.jetbrains.r.rinterop.RepoInstallPackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getRepoInstallPackageMethod(), responseObserver);
    }

    /**
     */
    public void repoAddLibraryPath(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getRepoAddLibraryPathMethod(), responseObserver);
    }

    /**
     */
    public void repoCheckPackageInstalled(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getRepoCheckPackageInstalledMethod(), responseObserver);
    }

    /**
     */
    public void repoRemovePackage(org.jetbrains.r.rinterop.RepoRemovePackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getRepoRemovePackageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    public void previewDataImport(org.jetbrains.r.rinterop.PreviewDataImportRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getPreviewDataImportMethod(), responseObserver);
    }

    /**
     */
    public void commitDataImport(org.jetbrains.r.rinterop.CommitDataImportRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getCommitDataImportMethod(), responseObserver);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public void copyToPersistentRef(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CopyToPersistentRefResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCopyToPersistentRefMethod(), responseObserver);
    }

    /**
     */
    public void disposePersistentRefs(org.jetbrains.r.rinterop.PersistentRefList request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisposePersistentRefsMethod(), responseObserver);
    }

    /**
     */
    public void loaderGetParentEnvs(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ParentEnvsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getLoaderGetParentEnvsMethod(), responseObserver);
    }

    /**
     */
    public void loaderGetVariables(org.jetbrains.r.rinterop.GetVariablesRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.VariablesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getLoaderGetVariablesMethod(), responseObserver);
    }

    /**
     */
    public void loaderGetLoadedNamespaces(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getLoaderGetLoadedNamespacesMethod(), responseObserver);
    }

    /**
     */
    public void loaderGetValueInfo(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getLoaderGetValueInfoMethod(), responseObserver);
    }

    /**
     */
    public void evaluateAsText(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringOrError> responseObserver) {
      asyncUnimplementedUnaryCall(getEvaluateAsTextMethod(), responseObserver);
    }

    /**
     */
    public void evaluateAsBoolean(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnimplementedUnaryCall(getEvaluateAsBooleanMethod(), responseObserver);
    }

    /**
     */
    public void getDistinctStrings(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDistinctStringsMethod(), responseObserver);
    }

    /**
     */
    public void loadObjectNames(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getLoadObjectNamesMethod(), responseObserver);
    }

    /**
     */
    public void findInheritorNamedArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getFindInheritorNamedArgumentsMethod(), responseObserver);
    }

    /**
     */
    public void findExtraNamedArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExtraNamedArguments> responseObserver) {
      asyncUnimplementedUnaryCall(getFindExtraNamedArgumentsMethod(), responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetS4ClassInfoByObjectNameMethod(), responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetR6ClassInfoByObjectNameMethod(), responseObserver);
    }

    /**
     */
    public void getTableColumnsInfo(org.jetbrains.r.rinterop.TableColumnsInfoRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.TableColumnsInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTableColumnsInfoMethod(), responseObserver);
    }

    /**
     */
    public void getFormalArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetFormalArgumentsMethod(), responseObserver);
    }

    /**
     */
    public void getEqualityObject(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value> responseObserver) {
      asyncUnimplementedUnaryCall(getGetEqualityObjectMethod(), responseObserver);
    }

    /**
     */
    public void setValue(org.jetbrains.r.rinterop.SetValueRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getSetValueMethod(), responseObserver);
    }

    /**
     */
    public void getObjectSizes(org.jetbrains.r.rinterop.RRefList request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.Int64List> responseObserver) {
      asyncUnimplementedUnaryCall(getGetObjectSizesMethod(), responseObserver);
    }

    /**
     */
    public void getRMarkdownChunkOptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetRMarkdownChunkOptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public void dataFrameRegister(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameRegisterMethod(), responseObserver);
    }

    /**
     */
    public void dataFrameGetInfo(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameGetInfoMethod(), responseObserver);
    }

    /**
     */
    public void dataFrameGetData(org.jetbrains.r.rinterop.DataFrameGetDataRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameGetDataResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameGetDataMethod(), responseObserver);
    }

    /**
     */
    public void dataFrameSort(org.jetbrains.r.rinterop.DataFrameSortRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameSortMethod(), responseObserver);
    }

    /**
     */
    public void dataFrameFilter(org.jetbrains.r.rinterop.DataFrameFilterRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameFilterMethod(), responseObserver);
    }

    /**
     */
    public void dataFrameRefresh(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnimplementedUnaryCall(getDataFrameRefreshMethod(), responseObserver);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public void convertRoxygenToHTML(org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getConvertRoxygenToHTMLMethod(), responseObserver);
    }

    /**
     */
    public void httpdRequest(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHttpdRequestMethod(), responseObserver);
    }

    /**
     */
    public void getDocumentationForPackage(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDocumentationForPackageMethod(), responseObserver);
    }

    /**
     */
    public void getDocumentationForSymbol(org.jetbrains.r.rinterop.DocumentationForSymbolRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDocumentationForSymbolMethod(), responseObserver);
    }

    /**
     */
    public void startHttpd(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnimplementedUnaryCall(getStartHttpdMethod(), responseObserver);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public void getWorkingDir(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnimplementedUnaryCall(getGetWorkingDirMethod(), responseObserver);
    }

    /**
     */
    public void setWorkingDir(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSetWorkingDirMethod(), responseObserver);
    }

    /**
     */
    public void clearEnvironment(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getClearEnvironmentMethod(), responseObserver);
    }

    /**
     */
    public void getSysEnv(org.jetbrains.r.rinterop.GetSysEnvRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetSysEnvMethod(), responseObserver);
    }

    /**
     */
    public void loadInstalledPackages(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RInstalledPackageList> responseObserver) {
      asyncUnimplementedUnaryCall(getLoadInstalledPackagesMethod(), responseObserver);
    }

    /**
     */
    public void loadLibPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RLibraryPathList> responseObserver) {
      asyncUnimplementedUnaryCall(getLoadLibPathsMethod(), responseObserver);
    }

    /**
     */
    public void loadLibrary(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getLoadLibraryMethod(), responseObserver);
    }

    /**
     */
    public void unloadLibrary(org.jetbrains.r.rinterop.UnloadLibraryRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getUnloadLibraryMethod(), responseObserver);
    }

    /**
     */
    public void saveGlobalEnvironment(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSaveGlobalEnvironmentMethod(), responseObserver);
    }

    /**
     */
    public void loadEnvironment(org.jetbrains.r.rinterop.LoadEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getLoadEnvironmentMethod(), responseObserver);
    }

    /**
     */
    public void setOutputWidth(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSetOutputWidthMethod(), responseObserver);
    }

    /**
     */
    public void clientRequestFinished(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getClientRequestFinishedMethod(), responseObserver);
    }

    /**
     */
    public void rStudioApiResponse(org.jetbrains.r.rinterop.RObject request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getRStudioApiResponseMethod(), responseObserver);
    }

    /**
     */
    public void setSaveOnExit(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getSetSaveOnExitMethod(), responseObserver);
    }

    /**
     */
    public void setRStudioApiEnabled(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncUnimplementedUnaryCall(getSetRStudioApiEnabledMethod(), responseObserver);
    }

    /**
     */
    public void getLoadedShortS4ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortS4ClassInfoList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLoadedShortS4ClassInfosMethod(), responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetS4ClassInfoByClassNameMethod(), responseObserver);
    }

    /**
     */
    public void getLoadedShortR6ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortR6ClassInfoList> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLoadedShortR6ClassInfosMethod(), responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo> responseObserver) {
      asyncUnimplementedUnaryCall(getGetR6ClassInfoByClassNameMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.GetInfoResponse>(
                  this, METHODID_GET_INFO)))
          .addMethod(
            getIsBusyMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.BoolValue>(
                  this, METHODID_IS_BUSY)))
          .addMethod(
            getInitMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.Init,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_INIT)))
          .addMethod(
            getQuitMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_QUIT)))
          .addMethod(
            getQuitProceedMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_QUIT_PROCEED)))
          .addMethod(
            getExecuteCodeMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.ExecuteCodeRequest,
                org.jetbrains.r.rinterop.ExecuteCodeResponse>(
                  this, METHODID_EXECUTE_CODE)))
          .addMethod(
            getSendReadLnMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.Empty>(
                  this, METHODID_SEND_READ_LN)))
          .addMethod(
            getSendEofMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_SEND_EOF)))
          .addMethod(
            getReplInterruptMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_REPL_INTERRUPT)))
          .addMethod(
            getGetAsyncEventsMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.AsyncEvent>(
                  this, METHODID_GET_ASYNC_EVENTS)))
          .addMethod(
            getDebugAddOrModifyBreakpointMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_ADD_OR_MODIFY_BREAKPOINT)))
          .addMethod(
            getDebugSetMasterBreakpointMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_SET_MASTER_BREAKPOINT)))
          .addMethod(
            getDebugRemoveBreakpointMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Int32Value,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_REMOVE_BREAKPOINT)))
          .addMethod(
            getDebugCommandContinueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_CONTINUE)))
          .addMethod(
            getDebugCommandPauseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_PAUSE)))
          .addMethod(
            getDebugCommandStopMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_STOP)))
          .addMethod(
            getDebugCommandStepOverMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_STEP_OVER)))
          .addMethod(
            getDebugCommandStepIntoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_STEP_INTO)))
          .addMethod(
            getDebugCommandStepIntoMyCodeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_STEP_INTO_MY_CODE)))
          .addMethod(
            getDebugCommandStepOutMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_STEP_OUT)))
          .addMethod(
            getDebugCommandRunToPositionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.SourcePosition,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_COMMAND_RUN_TO_POSITION)))
          .addMethod(
            getDebugMuteBreakpointsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.BoolValue,
                com.google.protobuf.Empty>(
                  this, METHODID_DEBUG_MUTE_BREAKPOINTS)))
          .addMethod(
            getGetFunctionSourcePositionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse>(
                  this, METHODID_GET_FUNCTION_SOURCE_POSITION)))
          .addMethod(
            getGetSourceFileTextMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.StringValue>(
                  this, METHODID_GET_SOURCE_FILE_TEXT)))
          .addMethod(
            getGetSourceFileNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.StringValue>(
                  this, METHODID_GET_SOURCE_FILE_NAME)))
          .addMethod(
            getGraphicsInitMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GraphicsInitRequest,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_INIT)))
          .addMethod(
            getGraphicsDumpMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.GraphicsDumpResponse>(
                  this, METHODID_GRAPHICS_DUMP)))
          .addMethod(
            getGraphicsRescaleMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GraphicsRescaleRequest,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_RESCALE)))
          .addMethod(
            getGraphicsRescaleStoredMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_RESCALE_STORED)))
          .addMethod(
            getGraphicsSetParametersMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.ScreenParameters,
                com.google.protobuf.Empty>(
                  this, METHODID_GRAPHICS_SET_PARAMETERS)))
          .addMethod(
            getGraphicsGetSnapshotPathMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest,
                org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse>(
                  this, METHODID_GRAPHICS_GET_SNAPSHOT_PATH)))
          .addMethod(
            getGraphicsFetchPlotMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Int32Value,
                org.jetbrains.r.rinterop.GraphicsFetchPlotResponse>(
                  this, METHODID_GRAPHICS_FETCH_PLOT)))
          .addMethod(
            getGraphicsCreateGroupMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_CREATE_GROUP)))
          .addMethod(
            getGraphicsRemoveGroupMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_REMOVE_GROUP)))
          .addMethod(
            getGraphicsShutdownMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_GRAPHICS_SHUTDOWN)))
          .addMethod(
            getBeforeChunkExecutionMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.ChunkParameters,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_BEFORE_CHUNK_EXECUTION)))
          .addMethod(
            getAfterChunkExecutionMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_AFTER_CHUNK_EXECUTION)))
          .addMethod(
            getPullChunkOutputPathsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_PULL_CHUNK_OUTPUT_PATHS)))
          .addMethod(
            getRepoGetPackageVersionMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_REPO_GET_PACKAGE_VERSION)))
          .addMethod(
            getRepoInstallPackageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RepoInstallPackageRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_REPO_INSTALL_PACKAGE)))
          .addMethod(
            getRepoAddLibraryPathMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_REPO_ADD_LIBRARY_PATH)))
          .addMethod(
            getRepoCheckPackageInstalledMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_REPO_CHECK_PACKAGE_INSTALLED)))
          .addMethod(
            getRepoRemovePackageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RepoRemovePackageRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_REPO_REMOVE_PACKAGE)))
          .addMethod(
            getPreviewDataImportMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.PreviewDataImportRequest,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_PREVIEW_DATA_IMPORT)))
          .addMethod(
            getCommitDataImportMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.CommitDataImportRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_COMMIT_DATA_IMPORT)))
          .addMethod(
            getCopyToPersistentRefMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.CopyToPersistentRefResponse>(
                  this, METHODID_COPY_TO_PERSISTENT_REF)))
          .addMethod(
            getDisposePersistentRefsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.PersistentRefList,
                com.google.protobuf.Empty>(
                  this, METHODID_DISPOSE_PERSISTENT_REFS)))
          .addMethod(
            getLoaderGetParentEnvsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.ParentEnvsResponse>(
                  this, METHODID_LOADER_GET_PARENT_ENVS)))
          .addMethod(
            getLoaderGetVariablesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GetVariablesRequest,
                org.jetbrains.r.rinterop.VariablesResponse>(
                  this, METHODID_LOADER_GET_VARIABLES)))
          .addMethod(
            getLoaderGetLoadedNamespacesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_LOADER_GET_LOADED_NAMESPACES)))
          .addMethod(
            getLoaderGetValueInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.ValueInfo>(
                  this, METHODID_LOADER_GET_VALUE_INFO)))
          .addMethod(
            getEvaluateAsTextMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.StringOrError>(
                  this, METHODID_EVALUATE_AS_TEXT)))
          .addMethod(
            getEvaluateAsBooleanMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                com.google.protobuf.BoolValue>(
                  this, METHODID_EVALUATE_AS_BOOLEAN)))
          .addMethod(
            getGetDistinctStringsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_GET_DISTINCT_STRINGS)))
          .addMethod(
            getLoadObjectNamesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_LOAD_OBJECT_NAMES)))
          .addMethod(
            getFindInheritorNamedArgumentsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_FIND_INHERITOR_NAMED_ARGUMENTS)))
          .addMethod(
            getFindExtraNamedArgumentsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.ExtraNamedArguments>(
                  this, METHODID_FIND_EXTRA_NAMED_ARGUMENTS)))
          .addMethod(
            getGetS4ClassInfoByObjectNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.classes.S4ClassInfo>(
                  this, METHODID_GET_S4CLASS_INFO_BY_OBJECT_NAME)))
          .addMethod(
            getGetR6ClassInfoByObjectNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.classes.R6ClassInfo>(
                  this, METHODID_GET_R6CLASS_INFO_BY_OBJECT_NAME)))
          .addMethod(
            getGetTableColumnsInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.TableColumnsInfoRequest,
                org.jetbrains.r.rinterop.TableColumnsInfo>(
                  this, METHODID_GET_TABLE_COLUMNS_INFO)))
          .addMethod(
            getGetFormalArgumentsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_GET_FORMAL_ARGUMENTS)))
          .addMethod(
            getGetEqualityObjectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                com.google.protobuf.Int64Value>(
                  this, METHODID_GET_EQUALITY_OBJECT)))
          .addMethod(
            getSetValueMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.SetValueRequest,
                org.jetbrains.r.rinterop.ValueInfo>(
                  this, METHODID_SET_VALUE)))
          .addMethod(
            getGetObjectSizesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRefList,
                org.jetbrains.r.rinterop.Int64List>(
                  this, METHODID_GET_OBJECT_SIZES)))
          .addMethod(
            getGetRMarkdownChunkOptionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_GET_RMARKDOWN_CHUNK_OPTIONS)))
          .addMethod(
            getDataFrameRegisterMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                com.google.protobuf.Int32Value>(
                  this, METHODID_DATA_FRAME_REGISTER)))
          .addMethod(
            getDataFrameGetInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                org.jetbrains.r.rinterop.DataFrameInfoResponse>(
                  this, METHODID_DATA_FRAME_GET_INFO)))
          .addMethod(
            getDataFrameGetDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DataFrameGetDataRequest,
                org.jetbrains.r.rinterop.DataFrameGetDataResponse>(
                  this, METHODID_DATA_FRAME_GET_DATA)))
          .addMethod(
            getDataFrameSortMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DataFrameSortRequest,
                com.google.protobuf.Int32Value>(
                  this, METHODID_DATA_FRAME_SORT)))
          .addMethod(
            getDataFrameFilterMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DataFrameFilterRequest,
                com.google.protobuf.Int32Value>(
                  this, METHODID_DATA_FRAME_FILTER)))
          .addMethod(
            getDataFrameRefreshMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                com.google.protobuf.BoolValue>(
                  this, METHODID_DATA_FRAME_REFRESH)))
          .addMethod(
            getConvertRoxygenToHTMLMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest,
                org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse>(
                  this, METHODID_CONVERT_ROXYGEN_TO_HTML)))
          .addMethod(
            getHttpdRequestMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.HttpdResponse>(
                  this, METHODID_HTTPD_REQUEST)))
          .addMethod(
            getGetDocumentationForPackageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.rinterop.HttpdResponse>(
                  this, METHODID_GET_DOCUMENTATION_FOR_PACKAGE)))
          .addMethod(
            getGetDocumentationForSymbolMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.DocumentationForSymbolRequest,
                org.jetbrains.r.rinterop.HttpdResponse>(
                  this, METHODID_GET_DOCUMENTATION_FOR_SYMBOL)))
          .addMethod(
            getStartHttpdMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Int32Value>(
                  this, METHODID_START_HTTPD)))
          .addMethod(
            getGetWorkingDirMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.StringValue>(
                  this, METHODID_GET_WORKING_DIR)))
          .addMethod(
            getSetWorkingDirMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.Empty>(
                  this, METHODID_SET_WORKING_DIR)))
          .addMethod(
            getClearEnvironmentMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RRef,
                com.google.protobuf.Empty>(
                  this, METHODID_CLEAR_ENVIRONMENT)))
          .addMethod(
            getGetSysEnvMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.GetSysEnvRequest,
                org.jetbrains.r.rinterop.StringList>(
                  this, METHODID_GET_SYS_ENV)))
          .addMethod(
            getLoadInstalledPackagesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.RInstalledPackageList>(
                  this, METHODID_LOAD_INSTALLED_PACKAGES)))
          .addMethod(
            getLoadLibPathsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.rinterop.RLibraryPathList>(
                  this, METHODID_LOAD_LIB_PATHS)))
          .addMethod(
            getLoadLibraryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.Empty>(
                  this, METHODID_LOAD_LIBRARY)))
          .addMethod(
            getUnloadLibraryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.UnloadLibraryRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_UNLOAD_LIBRARY)))
          .addMethod(
            getSaveGlobalEnvironmentMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                com.google.protobuf.Empty>(
                  this, METHODID_SAVE_GLOBAL_ENVIRONMENT)))
          .addMethod(
            getLoadEnvironmentMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.LoadEnvironmentRequest,
                com.google.protobuf.Empty>(
                  this, METHODID_LOAD_ENVIRONMENT)))
          .addMethod(
            getSetOutputWidthMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Int32Value,
                com.google.protobuf.Empty>(
                  this, METHODID_SET_OUTPUT_WIDTH)))
          .addMethod(
            getClientRequestFinishedMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_CLIENT_REQUEST_FINISHED)))
          .addMethod(
            getRStudioApiResponseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.jetbrains.r.rinterop.RObject,
                com.google.protobuf.Empty>(
                  this, METHODID_R_STUDIO_API_RESPONSE)))
          .addMethod(
            getSetSaveOnExitMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.BoolValue,
                com.google.protobuf.Empty>(
                  this, METHODID_SET_SAVE_ON_EXIT)))
          .addMethod(
            getSetRStudioApiEnabledMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.BoolValue,
                org.jetbrains.r.rinterop.CommandOutput>(
                  this, METHODID_SET_RSTUDIO_API_ENABLED)))
          .addMethod(
            getGetLoadedShortS4ClassInfosMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.classes.ShortS4ClassInfoList>(
                  this, METHODID_GET_LOADED_SHORT_S4CLASS_INFOS)))
          .addMethod(
            getGetS4ClassInfoByClassNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.classes.S4ClassInfo>(
                  this, METHODID_GET_S4CLASS_INFO_BY_CLASS_NAME)))
          .addMethod(
            getGetLoadedShortR6ClassInfosMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                org.jetbrains.r.classes.ShortR6ClassInfoList>(
                  this, METHODID_GET_LOADED_SHORT_R6CLASS_INFOS)))
          .addMethod(
            getGetR6ClassInfoByClassNameMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.StringValue,
                org.jetbrains.r.classes.R6ClassInfo>(
                  this, METHODID_GET_R6CLASS_INFO_BY_CLASS_NAME)))
          .build();
    }
  }

  /**
   */
  public static final class RPIServiceStub extends io.grpc.stub.AbstractAsyncStub<RPIServiceStub> {
    private RPIServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RPIServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RPIServiceStub(channel, callOptions);
    }

    /**
     */
    public void getInfo(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void isBusy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getIsBusyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void init(org.jetbrains.r.rinterop.Init request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getInitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void quit(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getQuitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void quitProceed(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getQuitProceedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeCode(org.jetbrains.r.rinterop.ExecuteCodeRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExecuteCodeResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getExecuteCodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendReadLn(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendReadLnMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendEof(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendEofMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void replInterrupt(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReplInterruptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAsyncEvents(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.AsyncEvent> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGetAsyncEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public void debugAddOrModifyBreakpoint(org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugAddOrModifyBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugSetMasterBreakpoint(org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugSetMasterBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugRemoveBreakpoint(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugRemoveBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandContinue(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandContinueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandPause(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandPauseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStop(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandStopMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepOver(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepOverMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepInto(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepIntoMyCode(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMyCodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepOut(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepOutMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandRunToPosition(org.jetbrains.r.rinterop.SourcePosition request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugCommandRunToPositionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugMuteBreakpoints(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDebugMuteBreakpointsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getFunctionSourcePosition(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetFunctionSourcePositionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSourceFileText(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetSourceFileTextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSourceFileName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetSourceFileNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    public void graphicsInit(org.jetbrains.r.rinterop.GraphicsInitRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsInitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsDump(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsDumpResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGraphicsDumpMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRescale(org.jetbrains.r.rinterop.GraphicsRescaleRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRescaleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRescaleStored(org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRescaleStoredMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsSetParameters(org.jetbrains.r.rinterop.ScreenParameters request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGraphicsSetParametersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsGetSnapshotPath(org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGraphicsGetSnapshotPathMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsFetchPlot(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGraphicsFetchPlotMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsCreateGroup(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsCreateGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRemoveGroup(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRemoveGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsShutdown(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGraphicsShutdownMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    public void beforeChunkExecution(org.jetbrains.r.rinterop.ChunkParameters request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getBeforeChunkExecutionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void afterChunkExecution(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getAfterChunkExecutionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pullChunkOutputPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPullChunkOutputPathsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    public void repoGetPackageVersion(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getRepoGetPackageVersionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoInstallPackage(org.jetbrains.r.rinterop.RepoInstallPackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRepoInstallPackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoAddLibraryPath(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getRepoAddLibraryPathMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoCheckPackageInstalled(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getRepoCheckPackageInstalledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoRemovePackage(org.jetbrains.r.rinterop.RepoRemovePackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRepoRemovePackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    public void previewDataImport(org.jetbrains.r.rinterop.PreviewDataImportRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getPreviewDataImportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commitDataImport(org.jetbrains.r.rinterop.CommitDataImportRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCommitDataImportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public void copyToPersistentRef(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CopyToPersistentRefResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCopyToPersistentRefMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void disposePersistentRefs(org.jetbrains.r.rinterop.PersistentRefList request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisposePersistentRefsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetParentEnvs(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ParentEnvsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoaderGetParentEnvsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetVariables(org.jetbrains.r.rinterop.GetVariablesRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.VariablesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoaderGetVariablesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetLoadedNamespaces(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoaderGetLoadedNamespacesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetValueInfo(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoaderGetValueInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void evaluateAsText(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringOrError> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEvaluateAsTextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void evaluateAsBoolean(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEvaluateAsBooleanMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDistinctStrings(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDistinctStringsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadObjectNames(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoadObjectNamesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void findInheritorNamedArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFindInheritorNamedArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void findExtraNamedArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExtraNamedArguments> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getFindExtraNamedArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByObjectNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByObjectNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTableColumnsInfo(org.jetbrains.r.rinterop.TableColumnsInfoRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.TableColumnsInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTableColumnsInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getFormalArguments(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetFormalArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEqualityObject(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetEqualityObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setValue(org.jetbrains.r.rinterop.SetValueRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetValueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getObjectSizes(org.jetbrains.r.rinterop.RRefList request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.Int64List> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetObjectSizesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getRMarkdownChunkOptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetRMarkdownChunkOptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public void dataFrameRegister(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameGetInfo(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameGetInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameGetData(org.jetbrains.r.rinterop.DataFrameGetDataRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameGetDataResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameGetDataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameSort(org.jetbrains.r.rinterop.DataFrameSortRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameSortMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameFilter(org.jetbrains.r.rinterop.DataFrameFilterRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameFilterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameRefresh(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDataFrameRefreshMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public void convertRoxygenToHTML(org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getConvertRoxygenToHTMLMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void httpdRequest(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHttpdRequestMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDocumentationForPackage(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDocumentationForPackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDocumentationForSymbol(org.jetbrains.r.rinterop.DocumentationForSymbolRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDocumentationForSymbolMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void startHttpd(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStartHttpdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public void getWorkingDir(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetWorkingDirMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setWorkingDir(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetWorkingDirMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clearEnvironment(org.jetbrains.r.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getClearEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSysEnv(org.jetbrains.r.rinterop.GetSysEnvRequest request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetSysEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadInstalledPackages(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RInstalledPackageList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoadInstalledPackagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadLibPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RLibraryPathList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoadLibPathsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadLibrary(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoadLibraryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void unloadLibrary(org.jetbrains.r.rinterop.UnloadLibraryRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUnloadLibraryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void saveGlobalEnvironment(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSaveGlobalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadEnvironment(org.jetbrains.r.rinterop.LoadEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getLoadEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setOutputWidth(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetOutputWidthMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clientRequestFinished(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getClientRequestFinishedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void rStudioApiResponse(org.jetbrains.r.rinterop.RObject request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRStudioApiResponseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setSaveOnExit(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSetSaveOnExitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setRStudioApiEnabled(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getSetRStudioApiEnabledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLoadedShortS4ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortS4ClassInfoList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLoadedShortS4ClassInfosMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByClassNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLoadedShortR6ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortR6ClassInfoList> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLoadedShortR6ClassInfosMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByClassNameMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class RPIServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<RPIServiceBlockingStub> {
    private RPIServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RPIServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RPIServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.jetbrains.r.rinterop.GetInfoResponse getInfo(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue isBusy(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getIsBusyMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> init(
        org.jetbrains.r.rinterop.Init request) {
      return blockingServerStreamingCall(
          getChannel(), getInitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty quit(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getQuitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty quitProceed(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getQuitProceedMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.ExecuteCodeResponse> executeCode(
        org.jetbrains.r.rinterop.ExecuteCodeRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getExecuteCodeMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty sendReadLn(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getSendReadLnMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty sendEof(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getSendEofMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty replInterrupt(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getReplInterruptMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.AsyncEvent> getAsyncEvents(
        com.google.protobuf.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), getGetAsyncEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public com.google.protobuf.Empty debugAddOrModifyBreakpoint(org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest request) {
      return blockingUnaryCall(
          getChannel(), getDebugAddOrModifyBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugSetMasterBreakpoint(org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest request) {
      return blockingUnaryCall(
          getChannel(), getDebugSetMasterBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugRemoveBreakpoint(com.google.protobuf.Int32Value request) {
      return blockingUnaryCall(
          getChannel(), getDebugRemoveBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandContinue(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandContinueMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandPause(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandPauseMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStop(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandStopMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepOver(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandStepOverMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepInto(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandStepIntoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepIntoMyCode(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandStepIntoMyCodeMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepOut(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandStepOutMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandRunToPosition(org.jetbrains.r.rinterop.SourcePosition request) {
      return blockingUnaryCall(
          getChannel(), getDebugCommandRunToPositionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugMuteBreakpoints(com.google.protobuf.BoolValue request) {
      return blockingUnaryCall(
          getChannel(), getDebugMuteBreakpointsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse getFunctionSourcePosition(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetFunctionSourcePositionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.StringValue getSourceFileText(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getGetSourceFileTextMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.StringValue getSourceFileName(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getGetSourceFileNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsInit(
        org.jetbrains.r.rinterop.GraphicsInitRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsInitMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.GraphicsDumpResponse graphicsDump(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGraphicsDumpMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsRescale(
        org.jetbrains.r.rinterop.GraphicsRescaleRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsRescaleMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsRescaleStored(
        org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsRescaleStoredMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty graphicsSetParameters(org.jetbrains.r.rinterop.ScreenParameters request) {
      return blockingUnaryCall(
          getChannel(), getGraphicsSetParametersMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse graphicsGetSnapshotPath(org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest request) {
      return blockingUnaryCall(
          getChannel(), getGraphicsGetSnapshotPathMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.GraphicsFetchPlotResponse graphicsFetchPlot(com.google.protobuf.Int32Value request) {
      return blockingUnaryCall(
          getChannel(), getGraphicsFetchPlotMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsCreateGroup(
        com.google.protobuf.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsCreateGroupMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsRemoveGroup(
        com.google.protobuf.StringValue request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsRemoveGroupMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> graphicsShutdown(
        com.google.protobuf.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), getGraphicsShutdownMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> beforeChunkExecution(
        org.jetbrains.r.rinterop.ChunkParameters request) {
      return blockingServerStreamingCall(
          getChannel(), getBeforeChunkExecutionMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> afterChunkExecution(
        com.google.protobuf.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), getAfterChunkExecutionMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList pullChunkOutputPaths(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getPullChunkOutputPathsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> repoGetPackageVersion(
        com.google.protobuf.StringValue request) {
      return blockingServerStreamingCall(
          getChannel(), getRepoGetPackageVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty repoInstallPackage(org.jetbrains.r.rinterop.RepoInstallPackageRequest request) {
      return blockingUnaryCall(
          getChannel(), getRepoInstallPackageMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> repoAddLibraryPath(
        com.google.protobuf.StringValue request) {
      return blockingServerStreamingCall(
          getChannel(), getRepoAddLibraryPathMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> repoCheckPackageInstalled(
        com.google.protobuf.StringValue request) {
      return blockingServerStreamingCall(
          getChannel(), getRepoCheckPackageInstalledMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty repoRemovePackage(org.jetbrains.r.rinterop.RepoRemovePackageRequest request) {
      return blockingUnaryCall(
          getChannel(), getRepoRemovePackageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> previewDataImport(
        org.jetbrains.r.rinterop.PreviewDataImportRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getPreviewDataImportMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty commitDataImport(org.jetbrains.r.rinterop.CommitDataImportRequest request) {
      return blockingUnaryCall(
          getChannel(), getCommitDataImportMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public org.jetbrains.r.rinterop.CopyToPersistentRefResponse copyToPersistentRef(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getCopyToPersistentRefMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty disposePersistentRefs(org.jetbrains.r.rinterop.PersistentRefList request) {
      return blockingUnaryCall(
          getChannel(), getDisposePersistentRefsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.ParentEnvsResponse loaderGetParentEnvs(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getLoaderGetParentEnvsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.VariablesResponse loaderGetVariables(org.jetbrains.r.rinterop.GetVariablesRequest request) {
      return blockingUnaryCall(
          getChannel(), getLoaderGetVariablesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList loaderGetLoadedNamespaces(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getLoaderGetLoadedNamespacesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.ValueInfo loaderGetValueInfo(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getLoaderGetValueInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringOrError evaluateAsText(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getEvaluateAsTextMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue evaluateAsBoolean(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getEvaluateAsBooleanMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList getDistinctStrings(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetDistinctStringsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList loadObjectNames(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getLoadObjectNamesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList findInheritorNamedArguments(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getFindInheritorNamedArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.ExtraNamedArguments findExtraNamedArguments(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getFindExtraNamedArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.S4ClassInfo getS4ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetS4ClassInfoByObjectNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.R6ClassInfo getR6ClassInfoByObjectName(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetR6ClassInfoByObjectNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.TableColumnsInfo getTableColumnsInfo(org.jetbrains.r.rinterop.TableColumnsInfoRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetTableColumnsInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList getFormalArguments(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetFormalArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int64Value getEqualityObject(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getGetEqualityObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.ValueInfo setValue(org.jetbrains.r.rinterop.SetValueRequest request) {
      return blockingUnaryCall(
          getChannel(), getSetValueMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.Int64List getObjectSizes(org.jetbrains.r.rinterop.RRefList request) {
      return blockingUnaryCall(
          getChannel(), getGetObjectSizesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList getRMarkdownChunkOptions(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetRMarkdownChunkOptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public com.google.protobuf.Int32Value dataFrameRegister(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameRegisterMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.DataFrameInfoResponse dataFrameGetInfo(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameGetInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.DataFrameGetDataResponse dataFrameGetData(org.jetbrains.r.rinterop.DataFrameGetDataRequest request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameGetDataMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value dataFrameSort(org.jetbrains.r.rinterop.DataFrameSortRequest request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameSortMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value dataFrameFilter(org.jetbrains.r.rinterop.DataFrameFilterRequest request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameFilterMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue dataFrameRefresh(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getDataFrameRefreshMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse convertRoxygenToHTML(org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest request) {
      return blockingUnaryCall(
          getChannel(), getConvertRoxygenToHTMLMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.HttpdResponse httpdRequest(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getHttpdRequestMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.HttpdResponse getDocumentationForPackage(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getGetDocumentationForPackageMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.HttpdResponse getDocumentationForSymbol(org.jetbrains.r.rinterop.DocumentationForSymbolRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetDocumentationForSymbolMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value startHttpd(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getStartHttpdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public com.google.protobuf.StringValue getWorkingDir(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetWorkingDirMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setWorkingDir(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getSetWorkingDirMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty clearEnvironment(org.jetbrains.r.rinterop.RRef request) {
      return blockingUnaryCall(
          getChannel(), getClearEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.StringList getSysEnv(org.jetbrains.r.rinterop.GetSysEnvRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetSysEnvMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.RInstalledPackageList loadInstalledPackages(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getLoadInstalledPackagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.rinterop.RLibraryPathList loadLibPaths(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getLoadLibPathsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty loadLibrary(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getLoadLibraryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty unloadLibrary(org.jetbrains.r.rinterop.UnloadLibraryRequest request) {
      return blockingUnaryCall(
          getChannel(), getUnloadLibraryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty saveGlobalEnvironment(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getSaveGlobalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty loadEnvironment(org.jetbrains.r.rinterop.LoadEnvironmentRequest request) {
      return blockingUnaryCall(
          getChannel(), getLoadEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setOutputWidth(com.google.protobuf.Int32Value request) {
      return blockingUnaryCall(
          getChannel(), getSetOutputWidthMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty clientRequestFinished(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getClientRequestFinishedMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty rStudioApiResponse(org.jetbrains.r.rinterop.RObject request) {
      return blockingUnaryCall(
          getChannel(), getRStudioApiResponseMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setSaveOnExit(com.google.protobuf.BoolValue request) {
      return blockingUnaryCall(
          getChannel(), getSetSaveOnExitMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<org.jetbrains.r.rinterop.CommandOutput> setRStudioApiEnabled(
        com.google.protobuf.BoolValue request) {
      return blockingServerStreamingCall(
          getChannel(), getSetRStudioApiEnabledMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.ShortS4ClassInfoList getLoadedShortS4ClassInfos(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetLoadedShortS4ClassInfosMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.S4ClassInfo getS4ClassInfoByClassName(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getGetS4ClassInfoByClassNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.ShortR6ClassInfoList getLoadedShortR6ClassInfos(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetLoadedShortR6ClassInfosMethod(), getCallOptions(), request);
    }

    /**
     */
    public org.jetbrains.r.classes.R6ClassInfo getR6ClassInfoByClassName(com.google.protobuf.StringValue request) {
      return blockingUnaryCall(
          getChannel(), getGetR6ClassInfoByClassNameMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class RPIServiceFutureStub extends io.grpc.stub.AbstractFutureStub<RPIServiceFutureStub> {
    private RPIServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RPIServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RPIServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.GetInfoResponse> getInfo(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> isBusy(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getIsBusyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> quit(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getQuitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> quitProceed(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getQuitProceedMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> sendReadLn(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getSendReadLnMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> sendEof(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getSendEofMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> replInterrupt(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getReplInterruptMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugAddOrModifyBreakpoint(
        org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugAddOrModifyBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugSetMasterBreakpoint(
        org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugSetMasterBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugRemoveBreakpoint(
        com.google.protobuf.Int32Value request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugRemoveBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandContinue(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandContinueMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandPause(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandPauseMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStop(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandStopMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepOver(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandStepOverMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepInto(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepIntoMyCode(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMyCodeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepOut(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandStepOutMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandRunToPosition(
        org.jetbrains.r.rinterop.SourcePosition request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugCommandRunToPositionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugMuteBreakpoints(
        com.google.protobuf.BoolValue request) {
      return futureUnaryCall(
          getChannel().newCall(getDebugMuteBreakpointsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse> getFunctionSourcePosition(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetFunctionSourcePositionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getSourceFileText(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getGetSourceFileTextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getSourceFileName(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getGetSourceFileNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.GraphicsDumpResponse> graphicsDump(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGraphicsDumpMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> graphicsSetParameters(
        org.jetbrains.r.rinterop.ScreenParameters request) {
      return futureUnaryCall(
          getChannel().newCall(getGraphicsSetParametersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse> graphicsGetSnapshotPath(
        org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGraphicsGetSnapshotPathMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.GraphicsFetchPlotResponse> graphicsFetchPlot(
        com.google.protobuf.Int32Value request) {
      return futureUnaryCall(
          getChannel().newCall(getGraphicsFetchPlotMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> pullChunkOutputPaths(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getPullChunkOutputPathsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> repoInstallPackage(
        org.jetbrains.r.rinterop.RepoInstallPackageRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRepoInstallPackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> repoRemovePackage(
        org.jetbrains.r.rinterop.RepoRemovePackageRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRepoRemovePackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> commitDataImport(
        org.jetbrains.r.rinterop.CommitDataImportRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCommitDataImportMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.CopyToPersistentRefResponse> copyToPersistentRef(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getCopyToPersistentRefMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> disposePersistentRefs(
        org.jetbrains.r.rinterop.PersistentRefList request) {
      return futureUnaryCall(
          getChannel().newCall(getDisposePersistentRefsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.ParentEnvsResponse> loaderGetParentEnvs(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getLoaderGetParentEnvsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.VariablesResponse> loaderGetVariables(
        org.jetbrains.r.rinterop.GetVariablesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getLoaderGetVariablesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> loaderGetLoadedNamespaces(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getLoaderGetLoadedNamespacesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.ValueInfo> loaderGetValueInfo(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getLoaderGetValueInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringOrError> evaluateAsText(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getEvaluateAsTextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> evaluateAsBoolean(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getEvaluateAsBooleanMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> getDistinctStrings(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDistinctStringsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> loadObjectNames(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getLoadObjectNamesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> findInheritorNamedArguments(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getFindInheritorNamedArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.ExtraNamedArguments> findExtraNamedArguments(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getFindExtraNamedArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.S4ClassInfo> getS4ClassInfoByObjectName(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByObjectNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.R6ClassInfo> getR6ClassInfoByObjectName(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByObjectNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.TableColumnsInfo> getTableColumnsInfo(
        org.jetbrains.r.rinterop.TableColumnsInfoRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTableColumnsInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> getFormalArguments(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetFormalArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int64Value> getEqualityObject(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getGetEqualityObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.ValueInfo> setValue(
        org.jetbrains.r.rinterop.SetValueRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSetValueMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.Int64List> getObjectSizes(
        org.jetbrains.r.rinterop.RRefList request) {
      return futureUnaryCall(
          getChannel().newCall(getGetObjectSizesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> getRMarkdownChunkOptions(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetRMarkdownChunkOptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameRegister(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameRegisterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.DataFrameInfoResponse> dataFrameGetInfo(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameGetInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.DataFrameGetDataResponse> dataFrameGetData(
        org.jetbrains.r.rinterop.DataFrameGetDataRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameGetDataMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameSort(
        org.jetbrains.r.rinterop.DataFrameSortRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameSortMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameFilter(
        org.jetbrains.r.rinterop.DataFrameFilterRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameFilterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> dataFrameRefresh(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getDataFrameRefreshMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse> convertRoxygenToHTML(
        org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getConvertRoxygenToHTMLMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.HttpdResponse> httpdRequest(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getHttpdRequestMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.HttpdResponse> getDocumentationForPackage(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDocumentationForPackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.HttpdResponse> getDocumentationForSymbol(
        org.jetbrains.r.rinterop.DocumentationForSymbolRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDocumentationForSymbolMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> startHttpd(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getStartHttpdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getWorkingDir(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetWorkingDirMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setWorkingDir(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getSetWorkingDirMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> clearEnvironment(
        org.jetbrains.r.rinterop.RRef request) {
      return futureUnaryCall(
          getChannel().newCall(getClearEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.StringList> getSysEnv(
        org.jetbrains.r.rinterop.GetSysEnvRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetSysEnvMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.RInstalledPackageList> loadInstalledPackages(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getLoadInstalledPackagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.rinterop.RLibraryPathList> loadLibPaths(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getLoadLibPathsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> loadLibrary(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getLoadLibraryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> unloadLibrary(
        org.jetbrains.r.rinterop.UnloadLibraryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUnloadLibraryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> saveGlobalEnvironment(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getSaveGlobalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> loadEnvironment(
        org.jetbrains.r.rinterop.LoadEnvironmentRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getLoadEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setOutputWidth(
        com.google.protobuf.Int32Value request) {
      return futureUnaryCall(
          getChannel().newCall(getSetOutputWidthMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> clientRequestFinished(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getClientRequestFinishedMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> rStudioApiResponse(
        org.jetbrains.r.rinterop.RObject request) {
      return futureUnaryCall(
          getChannel().newCall(getRStudioApiResponseMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setSaveOnExit(
        com.google.protobuf.BoolValue request) {
      return futureUnaryCall(
          getChannel().newCall(getSetSaveOnExitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.ShortS4ClassInfoList> getLoadedShortS4ClassInfos(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLoadedShortS4ClassInfosMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.S4ClassInfo> getS4ClassInfoByClassName(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByClassNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.ShortR6ClassInfoList> getLoadedShortR6ClassInfos(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLoadedShortR6ClassInfosMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.jetbrains.r.classes.R6ClassInfo> getR6ClassInfoByClassName(
        com.google.protobuf.StringValue request) {
      return futureUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByClassNameMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_INFO = 0;
  private static final int METHODID_IS_BUSY = 1;
  private static final int METHODID_INIT = 2;
  private static final int METHODID_QUIT = 3;
  private static final int METHODID_QUIT_PROCEED = 4;
  private static final int METHODID_EXECUTE_CODE = 5;
  private static final int METHODID_SEND_READ_LN = 6;
  private static final int METHODID_SEND_EOF = 7;
  private static final int METHODID_REPL_INTERRUPT = 8;
  private static final int METHODID_GET_ASYNC_EVENTS = 9;
  private static final int METHODID_DEBUG_ADD_OR_MODIFY_BREAKPOINT = 10;
  private static final int METHODID_DEBUG_SET_MASTER_BREAKPOINT = 11;
  private static final int METHODID_DEBUG_REMOVE_BREAKPOINT = 12;
  private static final int METHODID_DEBUG_COMMAND_CONTINUE = 13;
  private static final int METHODID_DEBUG_COMMAND_PAUSE = 14;
  private static final int METHODID_DEBUG_COMMAND_STOP = 15;
  private static final int METHODID_DEBUG_COMMAND_STEP_OVER = 16;
  private static final int METHODID_DEBUG_COMMAND_STEP_INTO = 17;
  private static final int METHODID_DEBUG_COMMAND_STEP_INTO_MY_CODE = 18;
  private static final int METHODID_DEBUG_COMMAND_STEP_OUT = 19;
  private static final int METHODID_DEBUG_COMMAND_RUN_TO_POSITION = 20;
  private static final int METHODID_DEBUG_MUTE_BREAKPOINTS = 21;
  private static final int METHODID_GET_FUNCTION_SOURCE_POSITION = 22;
  private static final int METHODID_GET_SOURCE_FILE_TEXT = 23;
  private static final int METHODID_GET_SOURCE_FILE_NAME = 24;
  private static final int METHODID_GRAPHICS_INIT = 25;
  private static final int METHODID_GRAPHICS_DUMP = 26;
  private static final int METHODID_GRAPHICS_RESCALE = 27;
  private static final int METHODID_GRAPHICS_RESCALE_STORED = 28;
  private static final int METHODID_GRAPHICS_SET_PARAMETERS = 29;
  private static final int METHODID_GRAPHICS_GET_SNAPSHOT_PATH = 30;
  private static final int METHODID_GRAPHICS_FETCH_PLOT = 31;
  private static final int METHODID_GRAPHICS_CREATE_GROUP = 32;
  private static final int METHODID_GRAPHICS_REMOVE_GROUP = 33;
  private static final int METHODID_GRAPHICS_SHUTDOWN = 34;
  private static final int METHODID_BEFORE_CHUNK_EXECUTION = 35;
  private static final int METHODID_AFTER_CHUNK_EXECUTION = 36;
  private static final int METHODID_PULL_CHUNK_OUTPUT_PATHS = 37;
  private static final int METHODID_REPO_GET_PACKAGE_VERSION = 38;
  private static final int METHODID_REPO_INSTALL_PACKAGE = 39;
  private static final int METHODID_REPO_ADD_LIBRARY_PATH = 40;
  private static final int METHODID_REPO_CHECK_PACKAGE_INSTALLED = 41;
  private static final int METHODID_REPO_REMOVE_PACKAGE = 42;
  private static final int METHODID_PREVIEW_DATA_IMPORT = 43;
  private static final int METHODID_COMMIT_DATA_IMPORT = 44;
  private static final int METHODID_COPY_TO_PERSISTENT_REF = 45;
  private static final int METHODID_DISPOSE_PERSISTENT_REFS = 46;
  private static final int METHODID_LOADER_GET_PARENT_ENVS = 47;
  private static final int METHODID_LOADER_GET_VARIABLES = 48;
  private static final int METHODID_LOADER_GET_LOADED_NAMESPACES = 49;
  private static final int METHODID_LOADER_GET_VALUE_INFO = 50;
  private static final int METHODID_EVALUATE_AS_TEXT = 51;
  private static final int METHODID_EVALUATE_AS_BOOLEAN = 52;
  private static final int METHODID_GET_DISTINCT_STRINGS = 53;
  private static final int METHODID_LOAD_OBJECT_NAMES = 54;
  private static final int METHODID_FIND_INHERITOR_NAMED_ARGUMENTS = 55;
  private static final int METHODID_FIND_EXTRA_NAMED_ARGUMENTS = 56;
  private static final int METHODID_GET_S4CLASS_INFO_BY_OBJECT_NAME = 57;
  private static final int METHODID_GET_R6CLASS_INFO_BY_OBJECT_NAME = 58;
  private static final int METHODID_GET_TABLE_COLUMNS_INFO = 59;
  private static final int METHODID_GET_FORMAL_ARGUMENTS = 60;
  private static final int METHODID_GET_EQUALITY_OBJECT = 61;
  private static final int METHODID_SET_VALUE = 62;
  private static final int METHODID_GET_OBJECT_SIZES = 63;
  private static final int METHODID_GET_RMARKDOWN_CHUNK_OPTIONS = 64;
  private static final int METHODID_DATA_FRAME_REGISTER = 65;
  private static final int METHODID_DATA_FRAME_GET_INFO = 66;
  private static final int METHODID_DATA_FRAME_GET_DATA = 67;
  private static final int METHODID_DATA_FRAME_SORT = 68;
  private static final int METHODID_DATA_FRAME_FILTER = 69;
  private static final int METHODID_DATA_FRAME_REFRESH = 70;
  private static final int METHODID_CONVERT_ROXYGEN_TO_HTML = 71;
  private static final int METHODID_HTTPD_REQUEST = 72;
  private static final int METHODID_GET_DOCUMENTATION_FOR_PACKAGE = 73;
  private static final int METHODID_GET_DOCUMENTATION_FOR_SYMBOL = 74;
  private static final int METHODID_START_HTTPD = 75;
  private static final int METHODID_GET_WORKING_DIR = 76;
  private static final int METHODID_SET_WORKING_DIR = 77;
  private static final int METHODID_CLEAR_ENVIRONMENT = 78;
  private static final int METHODID_GET_SYS_ENV = 79;
  private static final int METHODID_LOAD_INSTALLED_PACKAGES = 80;
  private static final int METHODID_LOAD_LIB_PATHS = 81;
  private static final int METHODID_LOAD_LIBRARY = 82;
  private static final int METHODID_UNLOAD_LIBRARY = 83;
  private static final int METHODID_SAVE_GLOBAL_ENVIRONMENT = 84;
  private static final int METHODID_LOAD_ENVIRONMENT = 85;
  private static final int METHODID_SET_OUTPUT_WIDTH = 86;
  private static final int METHODID_CLIENT_REQUEST_FINISHED = 87;
  private static final int METHODID_R_STUDIO_API_RESPONSE = 88;
  private static final int METHODID_SET_SAVE_ON_EXIT = 89;
  private static final int METHODID_SET_RSTUDIO_API_ENABLED = 90;
  private static final int METHODID_GET_LOADED_SHORT_S4CLASS_INFOS = 91;
  private static final int METHODID_GET_S4CLASS_INFO_BY_CLASS_NAME = 92;
  private static final int METHODID_GET_LOADED_SHORT_R6CLASS_INFOS = 93;
  private static final int METHODID_GET_R6CLASS_INFO_BY_CLASS_NAME = 94;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RPIServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RPIServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_INFO:
          serviceImpl.getInfo((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetInfoResponse>) responseObserver);
          break;
        case METHODID_IS_BUSY:
          serviceImpl.isBusy((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_INIT:
          serviceImpl.init((org.jetbrains.r.rinterop.Init) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_QUIT:
          serviceImpl.quit((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_QUIT_PROCEED:
          serviceImpl.quitProceed((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_EXECUTE_CODE:
          serviceImpl.executeCode((org.jetbrains.r.rinterop.ExecuteCodeRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExecuteCodeResponse>) responseObserver);
          break;
        case METHODID_SEND_READ_LN:
          serviceImpl.sendReadLn((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SEND_EOF:
          serviceImpl.sendEof((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REPL_INTERRUPT:
          serviceImpl.replInterrupt((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_ASYNC_EVENTS:
          serviceImpl.getAsyncEvents((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.AsyncEvent>) responseObserver);
          break;
        case METHODID_DEBUG_ADD_OR_MODIFY_BREAKPOINT:
          serviceImpl.debugAddOrModifyBreakpoint((org.jetbrains.r.rinterop.DebugAddOrModifyBreakpointRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_SET_MASTER_BREAKPOINT:
          serviceImpl.debugSetMasterBreakpoint((org.jetbrains.r.rinterop.DebugSetMasterBreakpointRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_REMOVE_BREAKPOINT:
          serviceImpl.debugRemoveBreakpoint((com.google.protobuf.Int32Value) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_CONTINUE:
          serviceImpl.debugCommandContinue((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_PAUSE:
          serviceImpl.debugCommandPause((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_STOP:
          serviceImpl.debugCommandStop((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_STEP_OVER:
          serviceImpl.debugCommandStepOver((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_STEP_INTO:
          serviceImpl.debugCommandStepInto((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_STEP_INTO_MY_CODE:
          serviceImpl.debugCommandStepIntoMyCode((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_STEP_OUT:
          serviceImpl.debugCommandStepOut((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_COMMAND_RUN_TO_POSITION:
          serviceImpl.debugCommandRunToPosition((org.jetbrains.r.rinterop.SourcePosition) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_MUTE_BREAKPOINTS:
          serviceImpl.debugMuteBreakpoints((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_FUNCTION_SOURCE_POSITION:
          serviceImpl.getFunctionSourcePosition((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GetFunctionSourcePositionResponse>) responseObserver);
          break;
        case METHODID_GET_SOURCE_FILE_TEXT:
          serviceImpl.getSourceFileText((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.StringValue>) responseObserver);
          break;
        case METHODID_GET_SOURCE_FILE_NAME:
          serviceImpl.getSourceFileName((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.StringValue>) responseObserver);
          break;
        case METHODID_GRAPHICS_INIT:
          serviceImpl.graphicsInit((org.jetbrains.r.rinterop.GraphicsInitRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_DUMP:
          serviceImpl.graphicsDump((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsDumpResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_RESCALE:
          serviceImpl.graphicsRescale((org.jetbrains.r.rinterop.GraphicsRescaleRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_RESCALE_STORED:
          serviceImpl.graphicsRescaleStored((org.jetbrains.r.rinterop.GraphicsRescaleStoredRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_SET_PARAMETERS:
          serviceImpl.graphicsSetParameters((org.jetbrains.r.rinterop.ScreenParameters) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GRAPHICS_GET_SNAPSHOT_PATH:
          serviceImpl.graphicsGetSnapshotPath((org.jetbrains.r.rinterop.GraphicsGetSnapshotPathRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsGetSnapshotPathResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_FETCH_PLOT:
          serviceImpl.graphicsFetchPlot((com.google.protobuf.Int32Value) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.GraphicsFetchPlotResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_CREATE_GROUP:
          serviceImpl.graphicsCreateGroup((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_REMOVE_GROUP:
          serviceImpl.graphicsRemoveGroup((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_SHUTDOWN:
          serviceImpl.graphicsShutdown((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_BEFORE_CHUNK_EXECUTION:
          serviceImpl.beforeChunkExecution((org.jetbrains.r.rinterop.ChunkParameters) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_AFTER_CHUNK_EXECUTION:
          serviceImpl.afterChunkExecution((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_PULL_CHUNK_OUTPUT_PATHS:
          serviceImpl.pullChunkOutputPaths((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_REPO_GET_PACKAGE_VERSION:
          serviceImpl.repoGetPackageVersion((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_INSTALL_PACKAGE:
          serviceImpl.repoInstallPackage((org.jetbrains.r.rinterop.RepoInstallPackageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REPO_ADD_LIBRARY_PATH:
          serviceImpl.repoAddLibraryPath((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_CHECK_PACKAGE_INSTALLED:
          serviceImpl.repoCheckPackageInstalled((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_REMOVE_PACKAGE:
          serviceImpl.repoRemovePackage((org.jetbrains.r.rinterop.RepoRemovePackageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_PREVIEW_DATA_IMPORT:
          serviceImpl.previewDataImport((org.jetbrains.r.rinterop.PreviewDataImportRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_COMMIT_DATA_IMPORT:
          serviceImpl.commitDataImport((org.jetbrains.r.rinterop.CommitDataImportRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_COPY_TO_PERSISTENT_REF:
          serviceImpl.copyToPersistentRef((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CopyToPersistentRefResponse>) responseObserver);
          break;
        case METHODID_DISPOSE_PERSISTENT_REFS:
          serviceImpl.disposePersistentRefs((org.jetbrains.r.rinterop.PersistentRefList) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LOADER_GET_PARENT_ENVS:
          serviceImpl.loaderGetParentEnvs((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ParentEnvsResponse>) responseObserver);
          break;
        case METHODID_LOADER_GET_VARIABLES:
          serviceImpl.loaderGetVariables((org.jetbrains.r.rinterop.GetVariablesRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.VariablesResponse>) responseObserver);
          break;
        case METHODID_LOADER_GET_LOADED_NAMESPACES:
          serviceImpl.loaderGetLoadedNamespaces((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOADER_GET_VALUE_INFO:
          serviceImpl.loaderGetValueInfo((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo>) responseObserver);
          break;
        case METHODID_EVALUATE_AS_TEXT:
          serviceImpl.evaluateAsText((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringOrError>) responseObserver);
          break;
        case METHODID_EVALUATE_AS_BOOLEAN:
          serviceImpl.evaluateAsBoolean((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_GET_DISTINCT_STRINGS:
          serviceImpl.getDistinctStrings((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOAD_OBJECT_NAMES:
          serviceImpl.loadObjectNames((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_FIND_INHERITOR_NAMED_ARGUMENTS:
          serviceImpl.findInheritorNamedArguments((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_FIND_EXTRA_NAMED_ARGUMENTS:
          serviceImpl.findExtraNamedArguments((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ExtraNamedArguments>) responseObserver);
          break;
        case METHODID_GET_S4CLASS_INFO_BY_OBJECT_NAME:
          serviceImpl.getS4ClassInfoByObjectName((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo>) responseObserver);
          break;
        case METHODID_GET_R6CLASS_INFO_BY_OBJECT_NAME:
          serviceImpl.getR6ClassInfoByObjectName((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo>) responseObserver);
          break;
        case METHODID_GET_TABLE_COLUMNS_INFO:
          serviceImpl.getTableColumnsInfo((org.jetbrains.r.rinterop.TableColumnsInfoRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.TableColumnsInfo>) responseObserver);
          break;
        case METHODID_GET_FORMAL_ARGUMENTS:
          serviceImpl.getFormalArguments((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_GET_EQUALITY_OBJECT:
          serviceImpl.getEqualityObject((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value>) responseObserver);
          break;
        case METHODID_SET_VALUE:
          serviceImpl.setValue((org.jetbrains.r.rinterop.SetValueRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ValueInfo>) responseObserver);
          break;
        case METHODID_GET_OBJECT_SIZES:
          serviceImpl.getObjectSizes((org.jetbrains.r.rinterop.RRefList) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.Int64List>) responseObserver);
          break;
        case METHODID_GET_RMARKDOWN_CHUNK_OPTIONS:
          serviceImpl.getRMarkdownChunkOptions((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_DATA_FRAME_REGISTER:
          serviceImpl.dataFrameRegister((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_GET_INFO:
          serviceImpl.dataFrameGetInfo((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameInfoResponse>) responseObserver);
          break;
        case METHODID_DATA_FRAME_GET_DATA:
          serviceImpl.dataFrameGetData((org.jetbrains.r.rinterop.DataFrameGetDataRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.DataFrameGetDataResponse>) responseObserver);
          break;
        case METHODID_DATA_FRAME_SORT:
          serviceImpl.dataFrameSort((org.jetbrains.r.rinterop.DataFrameSortRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_FILTER:
          serviceImpl.dataFrameFilter((org.jetbrains.r.rinterop.DataFrameFilterRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_REFRESH:
          serviceImpl.dataFrameRefresh((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_CONVERT_ROXYGEN_TO_HTML:
          serviceImpl.convertRoxygenToHTML((org.jetbrains.r.rinterop.ConvertRoxygenToHTMLRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.ConvertRoxygenToHTMLResponse>) responseObserver);
          break;
        case METHODID_HTTPD_REQUEST:
          serviceImpl.httpdRequest((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse>) responseObserver);
          break;
        case METHODID_GET_DOCUMENTATION_FOR_PACKAGE:
          serviceImpl.getDocumentationForPackage((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse>) responseObserver);
          break;
        case METHODID_GET_DOCUMENTATION_FOR_SYMBOL:
          serviceImpl.getDocumentationForSymbol((org.jetbrains.r.rinterop.DocumentationForSymbolRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.HttpdResponse>) responseObserver);
          break;
        case METHODID_START_HTTPD:
          serviceImpl.startHttpd((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_GET_WORKING_DIR:
          serviceImpl.getWorkingDir((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.StringValue>) responseObserver);
          break;
        case METHODID_SET_WORKING_DIR:
          serviceImpl.setWorkingDir((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_CLEAR_ENVIRONMENT:
          serviceImpl.clearEnvironment((org.jetbrains.r.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_SYS_ENV:
          serviceImpl.getSysEnv((org.jetbrains.r.rinterop.GetSysEnvRequest) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOAD_INSTALLED_PACKAGES:
          serviceImpl.loadInstalledPackages((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RInstalledPackageList>) responseObserver);
          break;
        case METHODID_LOAD_LIB_PATHS:
          serviceImpl.loadLibPaths((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.RLibraryPathList>) responseObserver);
          break;
        case METHODID_LOAD_LIBRARY:
          serviceImpl.loadLibrary((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_UNLOAD_LIBRARY:
          serviceImpl.unloadLibrary((org.jetbrains.r.rinterop.UnloadLibraryRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SAVE_GLOBAL_ENVIRONMENT:
          serviceImpl.saveGlobalEnvironment((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LOAD_ENVIRONMENT:
          serviceImpl.loadEnvironment((org.jetbrains.r.rinterop.LoadEnvironmentRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_OUTPUT_WIDTH:
          serviceImpl.setOutputWidth((com.google.protobuf.Int32Value) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_CLIENT_REQUEST_FINISHED:
          serviceImpl.clientRequestFinished((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_R_STUDIO_API_RESPONSE:
          serviceImpl.rStudioApiResponse((org.jetbrains.r.rinterop.RObject) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_SAVE_ON_EXIT:
          serviceImpl.setSaveOnExit((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_RSTUDIO_API_ENABLED:
          serviceImpl.setRStudioApiEnabled((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GET_LOADED_SHORT_S4CLASS_INFOS:
          serviceImpl.getLoadedShortS4ClassInfos((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortS4ClassInfoList>) responseObserver);
          break;
        case METHODID_GET_S4CLASS_INFO_BY_CLASS_NAME:
          serviceImpl.getS4ClassInfoByClassName((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.S4ClassInfo>) responseObserver);
          break;
        case METHODID_GET_LOADED_SHORT_R6CLASS_INFOS:
          serviceImpl.getLoadedShortR6ClassInfos((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.ShortR6ClassInfoList>) responseObserver);
          break;
        case METHODID_GET_R6CLASS_INFO_BY_CLASS_NAME:
          serviceImpl.getR6ClassInfoByClassName((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<org.jetbrains.r.classes.R6ClassInfo>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class RPIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RPIServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.jetbrains.r.rinterop.Service.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RPIService");
    }
  }

  private static final class RPIServiceFileDescriptorSupplier
      extends RPIServiceBaseDescriptorSupplier {
    RPIServiceFileDescriptorSupplier() {}
  }

  private static final class RPIServiceMethodDescriptorSupplier
      extends RPIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RPIServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RPIServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RPIServiceFileDescriptorSupplier())
              .addMethod(getGetInfoMethod())
              .addMethod(getIsBusyMethod())
              .addMethod(getInitMethod())
              .addMethod(getQuitMethod())
              .addMethod(getQuitProceedMethod())
              .addMethod(getExecuteCodeMethod())
              .addMethod(getSendReadLnMethod())
              .addMethod(getSendEofMethod())
              .addMethod(getReplInterruptMethod())
              .addMethod(getGetAsyncEventsMethod())
              .addMethod(getDebugAddOrModifyBreakpointMethod())
              .addMethod(getDebugSetMasterBreakpointMethod())
              .addMethod(getDebugRemoveBreakpointMethod())
              .addMethod(getDebugCommandContinueMethod())
              .addMethod(getDebugCommandPauseMethod())
              .addMethod(getDebugCommandStopMethod())
              .addMethod(getDebugCommandStepOverMethod())
              .addMethod(getDebugCommandStepIntoMethod())
              .addMethod(getDebugCommandStepIntoMyCodeMethod())
              .addMethod(getDebugCommandStepOutMethod())
              .addMethod(getDebugCommandRunToPositionMethod())
              .addMethod(getDebugMuteBreakpointsMethod())
              .addMethod(getGetFunctionSourcePositionMethod())
              .addMethod(getGetSourceFileTextMethod())
              .addMethod(getGetSourceFileNameMethod())
              .addMethod(getGraphicsInitMethod())
              .addMethod(getGraphicsDumpMethod())
              .addMethod(getGraphicsRescaleMethod())
              .addMethod(getGraphicsRescaleStoredMethod())
              .addMethod(getGraphicsSetParametersMethod())
              .addMethod(getGraphicsGetSnapshotPathMethod())
              .addMethod(getGraphicsFetchPlotMethod())
              .addMethod(getGraphicsCreateGroupMethod())
              .addMethod(getGraphicsRemoveGroupMethod())
              .addMethod(getGraphicsShutdownMethod())
              .addMethod(getBeforeChunkExecutionMethod())
              .addMethod(getAfterChunkExecutionMethod())
              .addMethod(getPullChunkOutputPathsMethod())
              .addMethod(getRepoGetPackageVersionMethod())
              .addMethod(getRepoInstallPackageMethod())
              .addMethod(getRepoAddLibraryPathMethod())
              .addMethod(getRepoCheckPackageInstalledMethod())
              .addMethod(getRepoRemovePackageMethod())
              .addMethod(getPreviewDataImportMethod())
              .addMethod(getCommitDataImportMethod())
              .addMethod(getCopyToPersistentRefMethod())
              .addMethod(getDisposePersistentRefsMethod())
              .addMethod(getLoaderGetParentEnvsMethod())
              .addMethod(getLoaderGetVariablesMethod())
              .addMethod(getLoaderGetLoadedNamespacesMethod())
              .addMethod(getLoaderGetValueInfoMethod())
              .addMethod(getEvaluateAsTextMethod())
              .addMethod(getEvaluateAsBooleanMethod())
              .addMethod(getGetDistinctStringsMethod())
              .addMethod(getLoadObjectNamesMethod())
              .addMethod(getFindInheritorNamedArgumentsMethod())
              .addMethod(getFindExtraNamedArgumentsMethod())
              .addMethod(getGetS4ClassInfoByObjectNameMethod())
              .addMethod(getGetR6ClassInfoByObjectNameMethod())
              .addMethod(getGetTableColumnsInfoMethod())
              .addMethod(getGetFormalArgumentsMethod())
              .addMethod(getGetEqualityObjectMethod())
              .addMethod(getSetValueMethod())
              .addMethod(getGetObjectSizesMethod())
              .addMethod(getGetRMarkdownChunkOptionsMethod())
              .addMethod(getDataFrameRegisterMethod())
              .addMethod(getDataFrameGetInfoMethod())
              .addMethod(getDataFrameGetDataMethod())
              .addMethod(getDataFrameSortMethod())
              .addMethod(getDataFrameFilterMethod())
              .addMethod(getDataFrameRefreshMethod())
              .addMethod(getConvertRoxygenToHTMLMethod())
              .addMethod(getHttpdRequestMethod())
              .addMethod(getGetDocumentationForPackageMethod())
              .addMethod(getGetDocumentationForSymbolMethod())
              .addMethod(getStartHttpdMethod())
              .addMethod(getGetWorkingDirMethod())
              .addMethod(getSetWorkingDirMethod())
              .addMethod(getClearEnvironmentMethod())
              .addMethod(getGetSysEnvMethod())
              .addMethod(getLoadInstalledPackagesMethod())
              .addMethod(getLoadLibPathsMethod())
              .addMethod(getLoadLibraryMethod())
              .addMethod(getUnloadLibraryMethod())
              .addMethod(getSaveGlobalEnvironmentMethod())
              .addMethod(getLoadEnvironmentMethod())
              .addMethod(getSetOutputWidthMethod())
              .addMethod(getClientRequestFinishedMethod())
              .addMethod(getRStudioApiResponseMethod())
              .addMethod(getSetSaveOnExitMethod())
              .addMethod(getSetRStudioApiEnabledMethod())
              .addMethod(getGetLoadedShortS4ClassInfosMethod())
              .addMethod(getGetS4ClassInfoByClassNameMethod())
              .addMethod(getGetLoadedShortR6ClassInfosMethod())
              .addMethod(getGetR6ClassInfoByClassNameMethod())
              .build();
        }
      }
    }
    return result;
  }
}
