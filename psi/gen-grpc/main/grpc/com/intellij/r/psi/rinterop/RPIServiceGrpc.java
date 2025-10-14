package com.intellij.r.psi.rinterop;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.57.2)",
    comments = "Source: service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RPIServiceGrpc {

  private RPIServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rplugininterop.RPIService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.GetInfoResponse> getGetInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getInfo",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.GetInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.GetInfoResponse> getGetInfoMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.GetInfoResponse> getGetInfoMethod;
    if ((getGetInfoMethod = RPIServiceGrpc.getGetInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetInfoMethod = RPIServiceGrpc.getGetInfoMethod) == null) {
          RPIServiceGrpc.getGetInfoMethod = getGetInfoMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.GetInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GetInfoResponse.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.Init,
      com.intellij.r.psi.rinterop.CommandOutput> getInitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "init",
      requestType = com.intellij.r.psi.rinterop.Init.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.Init,
      com.intellij.r.psi.rinterop.CommandOutput> getInitMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.Init, com.intellij.r.psi.rinterop.CommandOutput> getInitMethod;
    if ((getInitMethod = RPIServiceGrpc.getInitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getInitMethod = RPIServiceGrpc.getInitMethod) == null) {
          RPIServiceGrpc.getInitMethod = getInitMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.Init, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "init"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.Init.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ExecuteCodeRequest,
      com.intellij.r.psi.rinterop.ExecuteCodeResponse> getExecuteCodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "executeCode",
      requestType = com.intellij.r.psi.rinterop.ExecuteCodeRequest.class,
      responseType = com.intellij.r.psi.rinterop.ExecuteCodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ExecuteCodeRequest,
      com.intellij.r.psi.rinterop.ExecuteCodeResponse> getExecuteCodeMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ExecuteCodeRequest, com.intellij.r.psi.rinterop.ExecuteCodeResponse> getExecuteCodeMethod;
    if ((getExecuteCodeMethod = RPIServiceGrpc.getExecuteCodeMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getExecuteCodeMethod = RPIServiceGrpc.getExecuteCodeMethod) == null) {
          RPIServiceGrpc.getExecuteCodeMethod = getExecuteCodeMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.ExecuteCodeRequest, com.intellij.r.psi.rinterop.ExecuteCodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "executeCode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ExecuteCodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ExecuteCodeResponse.getDefaultInstance()))
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
      com.intellij.r.psi.rinterop.AsyncEvent> getGetAsyncEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAsyncEvents",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.AsyncEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.AsyncEvent> getGetAsyncEventsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.AsyncEvent> getGetAsyncEventsMethod;
    if ((getGetAsyncEventsMethod = RPIServiceGrpc.getGetAsyncEventsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetAsyncEventsMethod = RPIServiceGrpc.getGetAsyncEventsMethod) == null) {
          RPIServiceGrpc.getGetAsyncEventsMethod = getGetAsyncEventsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.AsyncEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAsyncEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.AsyncEvent.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getAsyncEvents"))
              .build();
        }
      }
    }
    return getGetAsyncEventsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest,
      com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugAddOrModifyBreakpoint",
      requestType = com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest,
      com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest, com.google.protobuf.Empty> getDebugAddOrModifyBreakpointMethod;
    if ((getDebugAddOrModifyBreakpointMethod = RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugAddOrModifyBreakpointMethod = RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod) == null) {
          RPIServiceGrpc.getDebugAddOrModifyBreakpointMethod = getDebugAddOrModifyBreakpointMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugAddOrModifyBreakpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("debugAddOrModifyBreakpoint"))
              .build();
        }
      }
    }
    return getDebugAddOrModifyBreakpointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest,
      com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugSetMasterBreakpoint",
      requestType = com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest,
      com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest, com.google.protobuf.Empty> getDebugSetMasterBreakpointMethod;
    if ((getDebugSetMasterBreakpointMethod = RPIServiceGrpc.getDebugSetMasterBreakpointMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugSetMasterBreakpointMethod = RPIServiceGrpc.getDebugSetMasterBreakpointMethod) == null) {
          RPIServiceGrpc.getDebugSetMasterBreakpointMethod = getDebugSetMasterBreakpointMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugSetMasterBreakpoint"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SourcePosition,
      com.google.protobuf.Empty> getDebugCommandRunToPositionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "debugCommandRunToPosition",
      requestType = com.intellij.r.psi.rinterop.SourcePosition.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SourcePosition,
      com.google.protobuf.Empty> getDebugCommandRunToPositionMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SourcePosition, com.google.protobuf.Empty> getDebugCommandRunToPositionMethod;
    if ((getDebugCommandRunToPositionMethod = RPIServiceGrpc.getDebugCommandRunToPositionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDebugCommandRunToPositionMethod = RPIServiceGrpc.getDebugCommandRunToPositionMethod) == null) {
          RPIServiceGrpc.getDebugCommandRunToPositionMethod = getDebugCommandRunToPositionMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.SourcePosition, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "debugCommandRunToPosition"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.SourcePosition.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getFunctionSourcePosition",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> getGetFunctionSourcePositionMethod;
    if ((getGetFunctionSourcePositionMethod = RPIServiceGrpc.getGetFunctionSourcePositionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetFunctionSourcePositionMethod = RPIServiceGrpc.getGetFunctionSourcePositionMethod) == null) {
          RPIServiceGrpc.getGetFunctionSourcePositionMethod = getGetFunctionSourcePositionMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getFunctionSourcePosition"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsInitRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsInitMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsInit",
      requestType = com.intellij.r.psi.rinterop.GraphicsInitRequest.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsInitRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsInitMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsInitRequest, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsInitMethod;
    if ((getGraphicsInitMethod = RPIServiceGrpc.getGraphicsInitMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsInitMethod = RPIServiceGrpc.getGraphicsInitMethod) == null) {
          RPIServiceGrpc.getGraphicsInitMethod = getGraphicsInitMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GraphicsInitRequest, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsInit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsInitRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsInit"))
              .build();
        }
      }
    }
    return getGraphicsInitMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsDump",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.GraphicsDumpResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.GraphicsDumpResponse> getGraphicsDumpMethod;
    if ((getGraphicsDumpMethod = RPIServiceGrpc.getGraphicsDumpMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsDumpMethod = RPIServiceGrpc.getGraphicsDumpMethod) == null) {
          RPIServiceGrpc.getGraphicsDumpMethod = getGraphicsDumpMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.GraphicsDumpResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsDump"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsDumpResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsDump"))
              .build();
        }
      }
    }
    return getGraphicsDumpMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRescale",
      requestType = com.intellij.r.psi.rinterop.GraphicsRescaleRequest.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleRequest, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleMethod;
    if ((getGraphicsRescaleMethod = RPIServiceGrpc.getGraphicsRescaleMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRescaleMethod = RPIServiceGrpc.getGraphicsRescaleMethod) == null) {
          RPIServiceGrpc.getGraphicsRescaleMethod = getGraphicsRescaleMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GraphicsRescaleRequest, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRescale"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsRescaleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRescale"))
              .build();
        }
      }
    }
    return getGraphicsRescaleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleStoredMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRescaleStored",
      requestType = com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleStoredMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRescaleStoredMethod;
    if ((getGraphicsRescaleStoredMethod = RPIServiceGrpc.getGraphicsRescaleStoredMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRescaleStoredMethod = RPIServiceGrpc.getGraphicsRescaleStoredMethod) == null) {
          RPIServiceGrpc.getGraphicsRescaleStoredMethod = getGraphicsRescaleStoredMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRescaleStored"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRescaleStored"))
              .build();
        }
      }
    }
    return getGraphicsRescaleStoredMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ScreenParameters,
      com.google.protobuf.Empty> getGraphicsSetParametersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsSetParameters",
      requestType = com.intellij.r.psi.rinterop.ScreenParameters.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ScreenParameters,
      com.google.protobuf.Empty> getGraphicsSetParametersMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ScreenParameters, com.google.protobuf.Empty> getGraphicsSetParametersMethod;
    if ((getGraphicsSetParametersMethod = RPIServiceGrpc.getGraphicsSetParametersMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsSetParametersMethod = RPIServiceGrpc.getGraphicsSetParametersMethod) == null) {
          RPIServiceGrpc.getGraphicsSetParametersMethod = getGraphicsSetParametersMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.ScreenParameters, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsSetParameters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ScreenParameters.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsSetParameters"))
              .build();
        }
      }
    }
    return getGraphicsSetParametersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest,
      com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsGetSnapshotPath",
      requestType = com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest.class,
      responseType = com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest,
      com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest, com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> getGraphicsGetSnapshotPathMethod;
    if ((getGraphicsGetSnapshotPathMethod = RPIServiceGrpc.getGraphicsGetSnapshotPathMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsGetSnapshotPathMethod = RPIServiceGrpc.getGraphicsGetSnapshotPathMethod) == null) {
          RPIServiceGrpc.getGraphicsGetSnapshotPathMethod = getGraphicsGetSnapshotPathMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest, com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsGetSnapshotPath"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsGetSnapshotPath"))
              .build();
        }
      }
    }
    return getGraphicsGetSnapshotPathMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsFetchPlot",
      requestType = com.google.protobuf.Int32Value.class,
      responseType = com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Int32Value,
      com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Int32Value, com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> getGraphicsFetchPlotMethod;
    if ((getGraphicsFetchPlotMethod = RPIServiceGrpc.getGraphicsFetchPlotMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsFetchPlotMethod = RPIServiceGrpc.getGraphicsFetchPlotMethod) == null) {
          RPIServiceGrpc.getGraphicsFetchPlotMethod = getGraphicsFetchPlotMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Int32Value, com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsFetchPlot"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsFetchPlot"))
              .build();
        }
      }
    }
    return getGraphicsFetchPlotMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsCreateGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsCreateGroup",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsCreateGroupMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsCreateGroupMethod;
    if ((getGraphicsCreateGroupMethod = RPIServiceGrpc.getGraphicsCreateGroupMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsCreateGroupMethod = RPIServiceGrpc.getGraphicsCreateGroupMethod) == null) {
          RPIServiceGrpc.getGraphicsCreateGroupMethod = getGraphicsCreateGroupMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsCreateGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsCreateGroup"))
              .build();
        }
      }
    }
    return getGraphicsCreateGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRemoveGroupMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsRemoveGroup",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRemoveGroupMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsRemoveGroupMethod;
    if ((getGraphicsRemoveGroupMethod = RPIServiceGrpc.getGraphicsRemoveGroupMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsRemoveGroupMethod = RPIServiceGrpc.getGraphicsRemoveGroupMethod) == null) {
          RPIServiceGrpc.getGraphicsRemoveGroupMethod = getGraphicsRemoveGroupMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsRemoveGroup"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsRemoveGroup"))
              .build();
        }
      }
    }
    return getGraphicsRemoveGroupMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsShutdownMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "graphicsShutdown",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getGraphicsShutdownMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput> getGraphicsShutdownMethod;
    if ((getGraphicsShutdownMethod = RPIServiceGrpc.getGraphicsShutdownMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGraphicsShutdownMethod = RPIServiceGrpc.getGraphicsShutdownMethod) == null) {
          RPIServiceGrpc.getGraphicsShutdownMethod = getGraphicsShutdownMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "graphicsShutdown"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("graphicsShutdown"))
              .build();
        }
      }
    }
    return getGraphicsShutdownMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ChunkParameters,
      com.intellij.r.psi.rinterop.CommandOutput> getBeforeChunkExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "beforeChunkExecution",
      requestType = com.intellij.r.psi.rinterop.ChunkParameters.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ChunkParameters,
      com.intellij.r.psi.rinterop.CommandOutput> getBeforeChunkExecutionMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ChunkParameters, com.intellij.r.psi.rinterop.CommandOutput> getBeforeChunkExecutionMethod;
    if ((getBeforeChunkExecutionMethod = RPIServiceGrpc.getBeforeChunkExecutionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getBeforeChunkExecutionMethod = RPIServiceGrpc.getBeforeChunkExecutionMethod) == null) {
          RPIServiceGrpc.getBeforeChunkExecutionMethod = getBeforeChunkExecutionMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.ChunkParameters, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "beforeChunkExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ChunkParameters.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("beforeChunkExecution"))
              .build();
        }
      }
    }
    return getBeforeChunkExecutionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getAfterChunkExecutionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "afterChunkExecution",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.CommandOutput> getAfterChunkExecutionMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput> getAfterChunkExecutionMethod;
    if ((getAfterChunkExecutionMethod = RPIServiceGrpc.getAfterChunkExecutionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getAfterChunkExecutionMethod = RPIServiceGrpc.getAfterChunkExecutionMethod) == null) {
          RPIServiceGrpc.getAfterChunkExecutionMethod = getAfterChunkExecutionMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "afterChunkExecution"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("afterChunkExecution"))
              .build();
        }
      }
    }
    return getAfterChunkExecutionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getPullChunkOutputPathsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "pullChunkOutputPaths",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getPullChunkOutputPathsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList> getPullChunkOutputPathsMethod;
    if ((getPullChunkOutputPathsMethod = RPIServiceGrpc.getPullChunkOutputPathsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getPullChunkOutputPathsMethod = RPIServiceGrpc.getPullChunkOutputPathsMethod) == null) {
          RPIServiceGrpc.getPullChunkOutputPathsMethod = getPullChunkOutputPathsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "pullChunkOutputPaths"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("pullChunkOutputPaths"))
              .build();
        }
      }
    }
    return getPullChunkOutputPathsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getRepoGetPackageVersionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoGetPackageVersion",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getRepoGetPackageVersionMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput> getRepoGetPackageVersionMethod;
    if ((getRepoGetPackageVersionMethod = RPIServiceGrpc.getRepoGetPackageVersionMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoGetPackageVersionMethod = RPIServiceGrpc.getRepoGetPackageVersionMethod) == null) {
          RPIServiceGrpc.getRepoGetPackageVersionMethod = getRepoGetPackageVersionMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoGetPackageVersion"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoGetPackageVersion"))
              .build();
        }
      }
    }
    return getRepoGetPackageVersionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoInstallPackageRequest,
      com.google.protobuf.Empty> getRepoInstallPackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoInstallPackage",
      requestType = com.intellij.r.psi.rinterop.RepoInstallPackageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoInstallPackageRequest,
      com.google.protobuf.Empty> getRepoInstallPackageMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoInstallPackageRequest, com.google.protobuf.Empty> getRepoInstallPackageMethod;
    if ((getRepoInstallPackageMethod = RPIServiceGrpc.getRepoInstallPackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoInstallPackageMethod = RPIServiceGrpc.getRepoInstallPackageMethod) == null) {
          RPIServiceGrpc.getRepoInstallPackageMethod = getRepoInstallPackageMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RepoInstallPackageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoInstallPackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RepoInstallPackageRequest.getDefaultInstance()))
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
      com.intellij.r.psi.rinterop.CommandOutput> getRepoAddLibraryPathMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoAddLibraryPath",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getRepoAddLibraryPathMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput> getRepoAddLibraryPathMethod;
    if ((getRepoAddLibraryPathMethod = RPIServiceGrpc.getRepoAddLibraryPathMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoAddLibraryPathMethod = RPIServiceGrpc.getRepoAddLibraryPathMethod) == null) {
          RPIServiceGrpc.getRepoAddLibraryPathMethod = getRepoAddLibraryPathMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoAddLibraryPath"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoAddLibraryPath"))
              .build();
        }
      }
    }
    return getRepoAddLibraryPathMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoCheckPackageInstalled",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput> getRepoCheckPackageInstalledMethod;
    if ((getRepoCheckPackageInstalledMethod = RPIServiceGrpc.getRepoCheckPackageInstalledMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoCheckPackageInstalledMethod = RPIServiceGrpc.getRepoCheckPackageInstalledMethod) == null) {
          RPIServiceGrpc.getRepoCheckPackageInstalledMethod = getRepoCheckPackageInstalledMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoCheckPackageInstalled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoCheckPackageInstalled"))
              .build();
        }
      }
    }
    return getRepoCheckPackageInstalledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoRemovePackageRequest,
      com.google.protobuf.Empty> getRepoRemovePackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "repoRemovePackage",
      requestType = com.intellij.r.psi.rinterop.RepoRemovePackageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoRemovePackageRequest,
      com.google.protobuf.Empty> getRepoRemovePackageMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RepoRemovePackageRequest, com.google.protobuf.Empty> getRepoRemovePackageMethod;
    if ((getRepoRemovePackageMethod = RPIServiceGrpc.getRepoRemovePackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRepoRemovePackageMethod = RPIServiceGrpc.getRepoRemovePackageMethod) == null) {
          RPIServiceGrpc.getRepoRemovePackageMethod = getRepoRemovePackageMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RepoRemovePackageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "repoRemovePackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RepoRemovePackageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("repoRemovePackage"))
              .build();
        }
      }
    }
    return getRepoRemovePackageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PreviewDataImportRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getPreviewDataImportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "previewDataImport",
      requestType = com.intellij.r.psi.rinterop.PreviewDataImportRequest.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PreviewDataImportRequest,
      com.intellij.r.psi.rinterop.CommandOutput> getPreviewDataImportMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PreviewDataImportRequest, com.intellij.r.psi.rinterop.CommandOutput> getPreviewDataImportMethod;
    if ((getPreviewDataImportMethod = RPIServiceGrpc.getPreviewDataImportMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getPreviewDataImportMethod = RPIServiceGrpc.getPreviewDataImportMethod) == null) {
          RPIServiceGrpc.getPreviewDataImportMethod = getPreviewDataImportMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.PreviewDataImportRequest, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "previewDataImport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.PreviewDataImportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("previewDataImport"))
              .build();
        }
      }
    }
    return getPreviewDataImportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.CommitDataImportRequest,
      com.google.protobuf.Empty> getCommitDataImportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "commitDataImport",
      requestType = com.intellij.r.psi.rinterop.CommitDataImportRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.CommitDataImportRequest,
      com.google.protobuf.Empty> getCommitDataImportMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.CommitDataImportRequest, com.google.protobuf.Empty> getCommitDataImportMethod;
    if ((getCommitDataImportMethod = RPIServiceGrpc.getCommitDataImportMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getCommitDataImportMethod = RPIServiceGrpc.getCommitDataImportMethod) == null) {
          RPIServiceGrpc.getCommitDataImportMethod = getCommitDataImportMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.CommitDataImportRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "commitDataImport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommitDataImportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("commitDataImport"))
              .build();
        }
      }
    }
    return getCommitDataImportMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "copyToPersistentRef",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.CopyToPersistentRefResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> getCopyToPersistentRefMethod;
    if ((getCopyToPersistentRefMethod = RPIServiceGrpc.getCopyToPersistentRefMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getCopyToPersistentRefMethod = RPIServiceGrpc.getCopyToPersistentRefMethod) == null) {
          RPIServiceGrpc.getCopyToPersistentRefMethod = getCopyToPersistentRefMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.CopyToPersistentRefResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "copyToPersistentRef"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CopyToPersistentRefResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("copyToPersistentRef"))
              .build();
        }
      }
    }
    return getCopyToPersistentRefMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PersistentRefList,
      com.google.protobuf.Empty> getDisposePersistentRefsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "disposePersistentRefs",
      requestType = com.intellij.r.psi.rinterop.PersistentRefList.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PersistentRefList,
      com.google.protobuf.Empty> getDisposePersistentRefsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.PersistentRefList, com.google.protobuf.Empty> getDisposePersistentRefsMethod;
    if ((getDisposePersistentRefsMethod = RPIServiceGrpc.getDisposePersistentRefsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDisposePersistentRefsMethod = RPIServiceGrpc.getDisposePersistentRefsMethod) == null) {
          RPIServiceGrpc.getDisposePersistentRefsMethod = getDisposePersistentRefsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.PersistentRefList, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "disposePersistentRefs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.PersistentRefList.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("disposePersistentRefs"))
              .build();
        }
      }
    }
    return getDisposePersistentRefsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetParentEnvs",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.ParentEnvsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ParentEnvsResponse> getLoaderGetParentEnvsMethod;
    if ((getLoaderGetParentEnvsMethod = RPIServiceGrpc.getLoaderGetParentEnvsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetParentEnvsMethod = RPIServiceGrpc.getLoaderGetParentEnvsMethod) == null) {
          RPIServiceGrpc.getLoaderGetParentEnvsMethod = getLoaderGetParentEnvsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ParentEnvsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetParentEnvs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ParentEnvsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetParentEnvs"))
              .build();
        }
      }
    }
    return getLoaderGetParentEnvsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetVariablesRequest,
      com.intellij.r.psi.rinterop.VariablesResponse> getLoaderGetVariablesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetVariables",
      requestType = com.intellij.r.psi.rinterop.GetVariablesRequest.class,
      responseType = com.intellij.r.psi.rinterop.VariablesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetVariablesRequest,
      com.intellij.r.psi.rinterop.VariablesResponse> getLoaderGetVariablesMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetVariablesRequest, com.intellij.r.psi.rinterop.VariablesResponse> getLoaderGetVariablesMethod;
    if ((getLoaderGetVariablesMethod = RPIServiceGrpc.getLoaderGetVariablesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetVariablesMethod = RPIServiceGrpc.getLoaderGetVariablesMethod) == null) {
          RPIServiceGrpc.getLoaderGetVariablesMethod = getLoaderGetVariablesMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GetVariablesRequest, com.intellij.r.psi.rinterop.VariablesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetVariables"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GetVariablesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.VariablesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetVariables"))
              .build();
        }
      }
    }
    return getLoaderGetVariablesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getLoaderGetLoadedNamespacesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetLoadedNamespaces",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getLoaderGetLoadedNamespacesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList> getLoaderGetLoadedNamespacesMethod;
    if ((getLoaderGetLoadedNamespacesMethod = RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetLoadedNamespacesMethod = RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod) == null) {
          RPIServiceGrpc.getLoaderGetLoadedNamespacesMethod = getLoaderGetLoadedNamespacesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetLoadedNamespaces"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetLoadedNamespaces"))
              .build();
        }
      }
    }
    return getLoaderGetLoadedNamespacesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ValueInfo> getLoaderGetValueInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loaderGetValueInfo",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.ValueInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ValueInfo> getLoaderGetValueInfoMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ValueInfo> getLoaderGetValueInfoMethod;
    if ((getLoaderGetValueInfoMethod = RPIServiceGrpc.getLoaderGetValueInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoaderGetValueInfoMethod = RPIServiceGrpc.getLoaderGetValueInfoMethod) == null) {
          RPIServiceGrpc.getLoaderGetValueInfoMethod = getLoaderGetValueInfoMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ValueInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loaderGetValueInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ValueInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loaderGetValueInfo"))
              .build();
        }
      }
    }
    return getLoaderGetValueInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringOrError> getEvaluateAsTextMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "evaluateAsText",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.StringOrError.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringOrError> getEvaluateAsTextMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringOrError> getEvaluateAsTextMethod;
    if ((getEvaluateAsTextMethod = RPIServiceGrpc.getEvaluateAsTextMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getEvaluateAsTextMethod = RPIServiceGrpc.getEvaluateAsTextMethod) == null) {
          RPIServiceGrpc.getEvaluateAsTextMethod = getEvaluateAsTextMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringOrError>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "evaluateAsText"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringOrError.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("evaluateAsText"))
              .build();
        }
      }
    }
    return getEvaluateAsTextMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "evaluateAsBoolean",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.google.protobuf.BoolValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.BoolValue> getEvaluateAsBooleanMethod;
    if ((getEvaluateAsBooleanMethod = RPIServiceGrpc.getEvaluateAsBooleanMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getEvaluateAsBooleanMethod = RPIServiceGrpc.getEvaluateAsBooleanMethod) == null) {
          RPIServiceGrpc.getEvaluateAsBooleanMethod = getEvaluateAsBooleanMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.BoolValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "evaluateAsBoolean"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("evaluateAsBoolean"))
              .build();
        }
      }
    }
    return getEvaluateAsBooleanMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getGetDistinctStringsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDistinctStrings",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getGetDistinctStringsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList> getGetDistinctStringsMethod;
    if ((getGetDistinctStringsMethod = RPIServiceGrpc.getGetDistinctStringsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDistinctStringsMethod = RPIServiceGrpc.getGetDistinctStringsMethod) == null) {
          RPIServiceGrpc.getGetDistinctStringsMethod = getGetDistinctStringsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDistinctStrings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getDistinctStrings"))
              .build();
        }
      }
    }
    return getGetDistinctStringsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getLoadObjectNamesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadObjectNames",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getLoadObjectNamesMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList> getLoadObjectNamesMethod;
    if ((getLoadObjectNamesMethod = RPIServiceGrpc.getLoadObjectNamesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadObjectNamesMethod = RPIServiceGrpc.getLoadObjectNamesMethod) == null) {
          RPIServiceGrpc.getLoadObjectNamesMethod = getLoadObjectNamesMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadObjectNames"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadObjectNames"))
              .build();
        }
      }
    }
    return getLoadObjectNamesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getFindInheritorNamedArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "findInheritorNamedArguments",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getFindInheritorNamedArgumentsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList> getFindInheritorNamedArgumentsMethod;
    if ((getFindInheritorNamedArgumentsMethod = RPIServiceGrpc.getFindInheritorNamedArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getFindInheritorNamedArgumentsMethod = RPIServiceGrpc.getFindInheritorNamedArgumentsMethod) == null) {
          RPIServiceGrpc.getFindInheritorNamedArgumentsMethod = getFindInheritorNamedArgumentsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "findInheritorNamedArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("findInheritorNamedArguments"))
              .build();
        }
      }
    }
    return getFindInheritorNamedArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "findExtraNamedArguments",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.ExtraNamedArguments.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ExtraNamedArguments> getFindExtraNamedArgumentsMethod;
    if ((getFindExtraNamedArgumentsMethod = RPIServiceGrpc.getFindExtraNamedArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getFindExtraNamedArgumentsMethod = RPIServiceGrpc.getFindExtraNamedArgumentsMethod) == null) {
          RPIServiceGrpc.getFindExtraNamedArgumentsMethod = getFindExtraNamedArgumentsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.ExtraNamedArguments>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "findExtraNamedArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ExtraNamedArguments.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("findExtraNamedArguments"))
              .build();
        }
      }
    }
    return getFindExtraNamedArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getS4ClassInfoByObjectName",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.classes.S4ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByObjectNameMethod;
    if ((getGetS4ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetS4ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod) == null) {
          RPIServiceGrpc.getGetS4ClassInfoByObjectNameMethod = getGetS4ClassInfoByObjectNameMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.classes.S4ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getS4ClassInfoByObjectName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.S4ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getS4ClassInfoByObjectName"))
              .build();
        }
      }
    }
    return getGetS4ClassInfoByObjectNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getR6ClassInfoByObjectName",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.classes.R6ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByObjectNameMethod;
    if ((getGetR6ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetR6ClassInfoByObjectNameMethod = RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod) == null) {
          RPIServiceGrpc.getGetR6ClassInfoByObjectNameMethod = getGetR6ClassInfoByObjectNameMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.classes.R6ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getR6ClassInfoByObjectName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.R6ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getR6ClassInfoByObjectName"))
              .build();
        }
      }
    }
    return getGetR6ClassInfoByObjectNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.TableColumnsInfoRequest,
      com.intellij.r.psi.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getTableColumnsInfo",
      requestType = com.intellij.r.psi.rinterop.TableColumnsInfoRequest.class,
      responseType = com.intellij.r.psi.rinterop.TableColumnsInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.TableColumnsInfoRequest,
      com.intellij.r.psi.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.TableColumnsInfoRequest, com.intellij.r.psi.rinterop.TableColumnsInfo> getGetTableColumnsInfoMethod;
    if ((getGetTableColumnsInfoMethod = RPIServiceGrpc.getGetTableColumnsInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetTableColumnsInfoMethod = RPIServiceGrpc.getGetTableColumnsInfoMethod) == null) {
          RPIServiceGrpc.getGetTableColumnsInfoMethod = getGetTableColumnsInfoMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.TableColumnsInfoRequest, com.intellij.r.psi.rinterop.TableColumnsInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getTableColumnsInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.TableColumnsInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.TableColumnsInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getTableColumnsInfo"))
              .build();
        }
      }
    }
    return getGetTableColumnsInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getGetFormalArgumentsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getFormalArguments",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.StringList> getGetFormalArgumentsMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList> getGetFormalArgumentsMethod;
    if ((getGetFormalArgumentsMethod = RPIServiceGrpc.getGetFormalArgumentsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetFormalArgumentsMethod = RPIServiceGrpc.getGetFormalArgumentsMethod) == null) {
          RPIServiceGrpc.getGetFormalArgumentsMethod = getGetFormalArgumentsMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getFormalArguments"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getFormalArguments"))
              .build();
        }
      }
    }
    return getGetFormalArgumentsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Int64Value> getGetEqualityObjectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getEqualityObject",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.google.protobuf.Int64Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Int64Value> getGetEqualityObjectMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Int64Value> getGetEqualityObjectMethod;
    if ((getGetEqualityObjectMethod = RPIServiceGrpc.getGetEqualityObjectMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetEqualityObjectMethod = RPIServiceGrpc.getGetEqualityObjectMethod) == null) {
          RPIServiceGrpc.getGetEqualityObjectMethod = getGetEqualityObjectMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Int64Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getEqualityObject"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int64Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getEqualityObject"))
              .build();
        }
      }
    }
    return getGetEqualityObjectMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SetValueRequest,
      com.intellij.r.psi.rinterop.ValueInfo> getSetValueMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setValue",
      requestType = com.intellij.r.psi.rinterop.SetValueRequest.class,
      responseType = com.intellij.r.psi.rinterop.ValueInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SetValueRequest,
      com.intellij.r.psi.rinterop.ValueInfo> getSetValueMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.SetValueRequest, com.intellij.r.psi.rinterop.ValueInfo> getSetValueMethod;
    if ((getSetValueMethod = RPIServiceGrpc.getSetValueMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetValueMethod = RPIServiceGrpc.getSetValueMethod) == null) {
          RPIServiceGrpc.getSetValueMethod = getSetValueMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.SetValueRequest, com.intellij.r.psi.rinterop.ValueInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setValue"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.SetValueRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ValueInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setValue"))
              .build();
        }
      }
    }
    return getSetValueMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRefList,
      com.intellij.r.psi.rinterop.Int64List> getGetObjectSizesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getObjectSizes",
      requestType = com.intellij.r.psi.rinterop.RRefList.class,
      responseType = com.intellij.r.psi.rinterop.Int64List.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRefList,
      com.intellij.r.psi.rinterop.Int64List> getGetObjectSizesMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRefList, com.intellij.r.psi.rinterop.Int64List> getGetObjectSizesMethod;
    if ((getGetObjectSizesMethod = RPIServiceGrpc.getGetObjectSizesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetObjectSizesMethod = RPIServiceGrpc.getGetObjectSizesMethod) == null) {
          RPIServiceGrpc.getGetObjectSizesMethod = getGetObjectSizesMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRefList, com.intellij.r.psi.rinterop.Int64List>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getObjectSizes"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRefList.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.Int64List.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getObjectSizes"))
              .build();
        }
      }
    }
    return getGetObjectSizesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getGetRMarkdownChunkOptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getRMarkdownChunkOptions",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.StringList> getGetRMarkdownChunkOptionsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList> getGetRMarkdownChunkOptionsMethod;
    if ((getGetRMarkdownChunkOptionsMethod = RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetRMarkdownChunkOptionsMethod = RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod) == null) {
          RPIServiceGrpc.getGetRMarkdownChunkOptionsMethod = getGetRMarkdownChunkOptionsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getRMarkdownChunkOptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getRMarkdownChunkOptions"))
              .build();
        }
      }
    }
    return getGetRMarkdownChunkOptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Int32Value> getDataFrameRegisterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameRegister",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Int32Value> getDataFrameRegisterMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Int32Value> getDataFrameRegisterMethod;
    if ((getDataFrameRegisterMethod = RPIServiceGrpc.getDataFrameRegisterMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameRegisterMethod = RPIServiceGrpc.getDataFrameRegisterMethod) == null) {
          RPIServiceGrpc.getDataFrameRegisterMethod = getDataFrameRegisterMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameRegister"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameRegister"))
              .build();
        }
      }
    }
    return getDataFrameRegisterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameGetInfo",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.intellij.r.psi.rinterop.DataFrameInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.intellij.r.psi.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.DataFrameInfoResponse> getDataFrameGetInfoMethod;
    if ((getDataFrameGetInfoMethod = RPIServiceGrpc.getDataFrameGetInfoMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameGetInfoMethod = RPIServiceGrpc.getDataFrameGetInfoMethod) == null) {
          RPIServiceGrpc.getDataFrameGetInfoMethod = getDataFrameGetInfoMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.intellij.r.psi.rinterop.DataFrameInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameGetInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DataFrameInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameGetInfo"))
              .build();
        }
      }
    }
    return getDataFrameGetInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameGetDataRequest,
      com.intellij.r.psi.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameGetData",
      requestType = com.intellij.r.psi.rinterop.DataFrameGetDataRequest.class,
      responseType = com.intellij.r.psi.rinterop.DataFrameGetDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameGetDataRequest,
      com.intellij.r.psi.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameGetDataRequest, com.intellij.r.psi.rinterop.DataFrameGetDataResponse> getDataFrameGetDataMethod;
    if ((getDataFrameGetDataMethod = RPIServiceGrpc.getDataFrameGetDataMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameGetDataMethod = RPIServiceGrpc.getDataFrameGetDataMethod) == null) {
          RPIServiceGrpc.getDataFrameGetDataMethod = getDataFrameGetDataMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DataFrameGetDataRequest, com.intellij.r.psi.rinterop.DataFrameGetDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameGetData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DataFrameGetDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DataFrameGetDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameGetData"))
              .build();
        }
      }
    }
    return getDataFrameGetDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameSortRequest,
      com.google.protobuf.Int32Value> getDataFrameSortMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameSort",
      requestType = com.intellij.r.psi.rinterop.DataFrameSortRequest.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameSortRequest,
      com.google.protobuf.Int32Value> getDataFrameSortMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameSortRequest, com.google.protobuf.Int32Value> getDataFrameSortMethod;
    if ((getDataFrameSortMethod = RPIServiceGrpc.getDataFrameSortMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameSortMethod = RPIServiceGrpc.getDataFrameSortMethod) == null) {
          RPIServiceGrpc.getDataFrameSortMethod = getDataFrameSortMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DataFrameSortRequest, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameSort"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DataFrameSortRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameSort"))
              .build();
        }
      }
    }
    return getDataFrameSortMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameFilterRequest,
      com.google.protobuf.Int32Value> getDataFrameFilterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameFilter",
      requestType = com.intellij.r.psi.rinterop.DataFrameFilterRequest.class,
      responseType = com.google.protobuf.Int32Value.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameFilterRequest,
      com.google.protobuf.Int32Value> getDataFrameFilterMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DataFrameFilterRequest, com.google.protobuf.Int32Value> getDataFrameFilterMethod;
    if ((getDataFrameFilterMethod = RPIServiceGrpc.getDataFrameFilterMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameFilterMethod = RPIServiceGrpc.getDataFrameFilterMethod) == null) {
          RPIServiceGrpc.getDataFrameFilterMethod = getDataFrameFilterMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DataFrameFilterRequest, com.google.protobuf.Int32Value>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameFilter"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DataFrameFilterRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Int32Value.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameFilter"))
              .build();
        }
      }
    }
    return getDataFrameFilterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.BoolValue> getDataFrameRefreshMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "dataFrameRefresh",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.google.protobuf.BoolValue.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.BoolValue> getDataFrameRefreshMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.BoolValue> getDataFrameRefreshMethod;
    if ((getDataFrameRefreshMethod = RPIServiceGrpc.getDataFrameRefreshMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getDataFrameRefreshMethod = RPIServiceGrpc.getDataFrameRefreshMethod) == null) {
          RPIServiceGrpc.getDataFrameRefreshMethod = getDataFrameRefreshMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.BoolValue>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "dataFrameRefresh"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("dataFrameRefresh"))
              .build();
        }
      }
    }
    return getDataFrameRefreshMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest,
      com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "convertRoxygenToHTML",
      requestType = com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest.class,
      responseType = com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest,
      com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest, com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> getConvertRoxygenToHTMLMethod;
    if ((getConvertRoxygenToHTMLMethod = RPIServiceGrpc.getConvertRoxygenToHTMLMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getConvertRoxygenToHTMLMethod = RPIServiceGrpc.getConvertRoxygenToHTMLMethod) == null) {
          RPIServiceGrpc.getConvertRoxygenToHTMLMethod = getConvertRoxygenToHTMLMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest, com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "convertRoxygenToHTML"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("convertRoxygenToHTML"))
              .build();
        }
      }
    }
    return getConvertRoxygenToHTMLMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.HttpdResponse> getHttpdRequestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "httpdRequest",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.HttpdResponse> getHttpdRequestMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.HttpdResponse> getHttpdRequestMethod;
    if ((getHttpdRequestMethod = RPIServiceGrpc.getHttpdRequestMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getHttpdRequestMethod = RPIServiceGrpc.getHttpdRequestMethod) == null) {
          RPIServiceGrpc.getHttpdRequestMethod = getHttpdRequestMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "httpdRequest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.HttpdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("httpdRequest"))
              .build();
        }
      }
    }
    return getHttpdRequestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForPackageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDocumentationForPackage",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForPackageMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForPackageMethod;
    if ((getGetDocumentationForPackageMethod = RPIServiceGrpc.getGetDocumentationForPackageMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDocumentationForPackageMethod = RPIServiceGrpc.getGetDocumentationForPackageMethod) == null) {
          RPIServiceGrpc.getGetDocumentationForPackageMethod = getGetDocumentationForPackageMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDocumentationForPackage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.HttpdResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getDocumentationForPackage"))
              .build();
        }
      }
    }
    return getGetDocumentationForPackageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DocumentationForSymbolRequest,
      com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getDocumentationForSymbol",
      requestType = com.intellij.r.psi.rinterop.DocumentationForSymbolRequest.class,
      responseType = com.intellij.r.psi.rinterop.HttpdResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DocumentationForSymbolRequest,
      com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.DocumentationForSymbolRequest, com.intellij.r.psi.rinterop.HttpdResponse> getGetDocumentationForSymbolMethod;
    if ((getGetDocumentationForSymbolMethod = RPIServiceGrpc.getGetDocumentationForSymbolMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetDocumentationForSymbolMethod = RPIServiceGrpc.getGetDocumentationForSymbolMethod) == null) {
          RPIServiceGrpc.getGetDocumentationForSymbolMethod = getGetDocumentationForSymbolMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.DocumentationForSymbolRequest, com.intellij.r.psi.rinterop.HttpdResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getDocumentationForSymbol"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.DocumentationForSymbolRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.HttpdResponse.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Empty> getClearEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "clearEnvironment",
      requestType = com.intellij.r.psi.rinterop.RRef.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef,
      com.google.protobuf.Empty> getClearEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Empty> getClearEnvironmentMethod;
    if ((getClearEnvironmentMethod = RPIServiceGrpc.getClearEnvironmentMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getClearEnvironmentMethod = RPIServiceGrpc.getClearEnvironmentMethod) == null) {
          RPIServiceGrpc.getClearEnvironmentMethod = getClearEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RRef, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "clearEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RRef.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("clearEnvironment"))
              .build();
        }
      }
    }
    return getClearEnvironmentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetSysEnvRequest,
      com.intellij.r.psi.rinterop.StringList> getGetSysEnvMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getSysEnv",
      requestType = com.intellij.r.psi.rinterop.GetSysEnvRequest.class,
      responseType = com.intellij.r.psi.rinterop.StringList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetSysEnvRequest,
      com.intellij.r.psi.rinterop.StringList> getGetSysEnvMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.GetSysEnvRequest, com.intellij.r.psi.rinterop.StringList> getGetSysEnvMethod;
    if ((getGetSysEnvMethod = RPIServiceGrpc.getGetSysEnvMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetSysEnvMethod = RPIServiceGrpc.getGetSysEnvMethod) == null) {
          RPIServiceGrpc.getGetSysEnvMethod = getGetSysEnvMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.GetSysEnvRequest, com.intellij.r.psi.rinterop.StringList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getSysEnv"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.GetSysEnvRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.StringList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getSysEnv"))
              .build();
        }
      }
    }
    return getGetSysEnvMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadInstalledPackages",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.RInstalledPackageList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.RInstalledPackageList> getLoadInstalledPackagesMethod;
    if ((getLoadInstalledPackagesMethod = RPIServiceGrpc.getLoadInstalledPackagesMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadInstalledPackagesMethod = RPIServiceGrpc.getLoadInstalledPackagesMethod) == null) {
          RPIServiceGrpc.getLoadInstalledPackagesMethod = getLoadInstalledPackagesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.RInstalledPackageList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadInstalledPackages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RInstalledPackageList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("loadInstalledPackages"))
              .build();
        }
      }
    }
    return getLoadInstalledPackagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.RLibraryPathList> getLoadLibPathsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadLibPaths",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.rinterop.RLibraryPathList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.rinterop.RLibraryPathList> getLoadLibPathsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.RLibraryPathList> getLoadLibPathsMethod;
    if ((getLoadLibPathsMethod = RPIServiceGrpc.getLoadLibPathsMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadLibPathsMethod = RPIServiceGrpc.getLoadLibPathsMethod) == null) {
          RPIServiceGrpc.getLoadLibPathsMethod = getLoadLibPathsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.rinterop.RLibraryPathList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadLibPaths"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RLibraryPathList.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.UnloadLibraryRequest,
      com.google.protobuf.Empty> getUnloadLibraryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "unloadLibrary",
      requestType = com.intellij.r.psi.rinterop.UnloadLibraryRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.UnloadLibraryRequest,
      com.google.protobuf.Empty> getUnloadLibraryMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.UnloadLibraryRequest, com.google.protobuf.Empty> getUnloadLibraryMethod;
    if ((getUnloadLibraryMethod = RPIServiceGrpc.getUnloadLibraryMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getUnloadLibraryMethod = RPIServiceGrpc.getUnloadLibraryMethod) == null) {
          RPIServiceGrpc.getUnloadLibraryMethod = getUnloadLibraryMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.UnloadLibraryRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "unloadLibrary"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.UnloadLibraryRequest.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.LoadEnvironmentRequest,
      com.google.protobuf.Empty> getLoadEnvironmentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "loadEnvironment",
      requestType = com.intellij.r.psi.rinterop.LoadEnvironmentRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.LoadEnvironmentRequest,
      com.google.protobuf.Empty> getLoadEnvironmentMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.LoadEnvironmentRequest, com.google.protobuf.Empty> getLoadEnvironmentMethod;
    if ((getLoadEnvironmentMethod = RPIServiceGrpc.getLoadEnvironmentMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getLoadEnvironmentMethod = RPIServiceGrpc.getLoadEnvironmentMethod) == null) {
          RPIServiceGrpc.getLoadEnvironmentMethod = getLoadEnvironmentMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.LoadEnvironmentRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "loadEnvironment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.LoadEnvironmentRequest.getDefaultInstance()))
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

  private static volatile io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RObject,
      com.google.protobuf.Empty> getRStudioApiResponseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "rStudioApiResponse",
      requestType = com.intellij.r.psi.rinterop.RObject.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RObject,
      com.google.protobuf.Empty> getRStudioApiResponseMethod() {
    io.grpc.MethodDescriptor<com.intellij.r.psi.rinterop.RObject, com.google.protobuf.Empty> getRStudioApiResponseMethod;
    if ((getRStudioApiResponseMethod = RPIServiceGrpc.getRStudioApiResponseMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getRStudioApiResponseMethod = RPIServiceGrpc.getRStudioApiResponseMethod) == null) {
          RPIServiceGrpc.getRStudioApiResponseMethod = getRStudioApiResponseMethod =
              io.grpc.MethodDescriptor.<com.intellij.r.psi.rinterop.RObject, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "rStudioApiResponse"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.RObject.getDefaultInstance()))
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
      com.intellij.r.psi.rinterop.CommandOutput> getSetRStudioApiEnabledMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "setRStudioApiEnabled",
      requestType = com.google.protobuf.BoolValue.class,
      responseType = com.intellij.r.psi.rinterop.CommandOutput.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.BoolValue,
      com.intellij.r.psi.rinterop.CommandOutput> getSetRStudioApiEnabledMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.BoolValue, com.intellij.r.psi.rinterop.CommandOutput> getSetRStudioApiEnabledMethod;
    if ((getSetRStudioApiEnabledMethod = RPIServiceGrpc.getSetRStudioApiEnabledMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getSetRStudioApiEnabledMethod = RPIServiceGrpc.getSetRStudioApiEnabledMethod) == null) {
          RPIServiceGrpc.getSetRStudioApiEnabledMethod = getSetRStudioApiEnabledMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.BoolValue, com.intellij.r.psi.rinterop.CommandOutput>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "setRStudioApiEnabled"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.BoolValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.rinterop.CommandOutput.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("setRStudioApiEnabled"))
              .build();
        }
      }
    }
    return getSetRStudioApiEnabledMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLoadedShortS4ClassInfos",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.classes.ShortS4ClassInfoList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.classes.ShortS4ClassInfoList> getGetLoadedShortS4ClassInfosMethod;
    if ((getGetLoadedShortS4ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetLoadedShortS4ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod) == null) {
          RPIServiceGrpc.getGetLoadedShortS4ClassInfosMethod = getGetLoadedShortS4ClassInfosMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.classes.ShortS4ClassInfoList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLoadedShortS4ClassInfos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.ShortS4ClassInfoList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getLoadedShortS4ClassInfos"))
              .build();
        }
      }
    }
    return getGetLoadedShortS4ClassInfosMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getS4ClassInfoByClassName",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.classes.S4ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.classes.S4ClassInfo> getGetS4ClassInfoByClassNameMethod;
    if ((getGetS4ClassInfoByClassNameMethod = RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetS4ClassInfoByClassNameMethod = RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod) == null) {
          RPIServiceGrpc.getGetS4ClassInfoByClassNameMethod = getGetS4ClassInfoByClassNameMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.classes.S4ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getS4ClassInfoByClassName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.S4ClassInfo.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getS4ClassInfoByClassName"))
              .build();
        }
      }
    }
    return getGetS4ClassInfoByClassNameMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getLoadedShortR6ClassInfos",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.intellij.r.psi.classes.ShortR6ClassInfoList.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.intellij.r.psi.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.intellij.r.psi.classes.ShortR6ClassInfoList> getGetLoadedShortR6ClassInfosMethod;
    if ((getGetLoadedShortR6ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetLoadedShortR6ClassInfosMethod = RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod) == null) {
          RPIServiceGrpc.getGetLoadedShortR6ClassInfosMethod = getGetLoadedShortR6ClassInfosMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.intellij.r.psi.classes.ShortR6ClassInfoList>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getLoadedShortR6ClassInfos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.ShortR6ClassInfoList.getDefaultInstance()))
              .setSchemaDescriptor(new RPIServiceMethodDescriptorSupplier("getLoadedShortR6ClassInfos"))
              .build();
        }
      }
    }
    return getGetLoadedShortR6ClassInfosMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getR6ClassInfoByClassName",
      requestType = com.google.protobuf.StringValue.class,
      responseType = com.intellij.r.psi.classes.R6ClassInfo.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.StringValue,
      com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.StringValue, com.intellij.r.psi.classes.R6ClassInfo> getGetR6ClassInfoByClassNameMethod;
    if ((getGetR6ClassInfoByClassNameMethod = RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod) == null) {
      synchronized (RPIServiceGrpc.class) {
        if ((getGetR6ClassInfoByClassNameMethod = RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod) == null) {
          RPIServiceGrpc.getGetR6ClassInfoByClassNameMethod = getGetR6ClassInfoByClassNameMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.StringValue, com.intellij.r.psi.classes.R6ClassInfo>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getR6ClassInfoByClassName"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.StringValue.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.intellij.r.psi.classes.R6ClassInfo.getDefaultInstance()))
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
  public interface AsyncService {

    /**
     */
    default void getInfo(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetInfoMethod(), responseObserver);
    }

    /**
     */
    default void isBusy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getIsBusyMethod(), responseObserver);
    }

    /**
     */
    default void init(com.intellij.r.psi.rinterop.Init request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInitMethod(), responseObserver);
    }

    /**
     */
    default void quit(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQuitMethod(), responseObserver);
    }

    /**
     */
    default void quitProceed(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getQuitProceedMethod(), responseObserver);
    }

    /**
     */
    default void executeCode(com.intellij.r.psi.rinterop.ExecuteCodeRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExecuteCodeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExecuteCodeMethod(), responseObserver);
    }

    /**
     */
    default void sendReadLn(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendReadLnMethod(), responseObserver);
    }

    /**
     */
    default void sendEof(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendEofMethod(), responseObserver);
    }

    /**
     */
    default void replInterrupt(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReplInterruptMethod(), responseObserver);
    }

    /**
     */
    default void getAsyncEvents(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.AsyncEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAsyncEventsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    default void debugAddOrModifyBreakpoint(com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugAddOrModifyBreakpointMethod(), responseObserver);
    }

    /**
     */
    default void debugSetMasterBreakpoint(com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugSetMasterBreakpointMethod(), responseObserver);
    }

    /**
     */
    default void debugRemoveBreakpoint(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugRemoveBreakpointMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandContinue(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandContinueMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandPause(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandPauseMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandStop(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandStopMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandStepOver(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandStepOverMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandStepInto(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandStepIntoMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandStepIntoMyCode(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandStepIntoMyCodeMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandStepOut(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandStepOutMethod(), responseObserver);
    }

    /**
     */
    default void debugCommandRunToPosition(com.intellij.r.psi.rinterop.SourcePosition request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugCommandRunToPositionMethod(), responseObserver);
    }

    /**
     */
    default void debugMuteBreakpoints(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDebugMuteBreakpointsMethod(), responseObserver);
    }

    /**
     */
    default void getFunctionSourcePosition(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetFunctionSourcePositionMethod(), responseObserver);
    }

    /**
     */
    default void getSourceFileText(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSourceFileTextMethod(), responseObserver);
    }

    /**
     */
    default void getSourceFileName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSourceFileNameMethod(), responseObserver);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    default void graphicsInit(com.intellij.r.psi.rinterop.GraphicsInitRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsInitMethod(), responseObserver);
    }

    /**
     */
    default void graphicsDump(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsDumpResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsDumpMethod(), responseObserver);
    }

    /**
     */
    default void graphicsRescale(com.intellij.r.psi.rinterop.GraphicsRescaleRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsRescaleMethod(), responseObserver);
    }

    /**
     */
    default void graphicsRescaleStored(com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsRescaleStoredMethod(), responseObserver);
    }

    /**
     */
    default void graphicsSetParameters(com.intellij.r.psi.rinterop.ScreenParameters request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsSetParametersMethod(), responseObserver);
    }

    /**
     */
    default void graphicsGetSnapshotPath(com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsGetSnapshotPathMethod(), responseObserver);
    }

    /**
     */
    default void graphicsFetchPlot(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsFetchPlotMethod(), responseObserver);
    }

    /**
     */
    default void graphicsCreateGroup(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsCreateGroupMethod(), responseObserver);
    }

    /**
     */
    default void graphicsRemoveGroup(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsRemoveGroupMethod(), responseObserver);
    }

    /**
     */
    default void graphicsShutdown(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGraphicsShutdownMethod(), responseObserver);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    default void beforeChunkExecution(com.intellij.r.psi.rinterop.ChunkParameters request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBeforeChunkExecutionMethod(), responseObserver);
    }

    /**
     */
    default void afterChunkExecution(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAfterChunkExecutionMethod(), responseObserver);
    }

    /**
     */
    default void pullChunkOutputPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPullChunkOutputPathsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    default void repoGetPackageVersion(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRepoGetPackageVersionMethod(), responseObserver);
    }

    /**
     */
    default void repoInstallPackage(com.intellij.r.psi.rinterop.RepoInstallPackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRepoInstallPackageMethod(), responseObserver);
    }

    /**
     */
    default void repoAddLibraryPath(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRepoAddLibraryPathMethod(), responseObserver);
    }

    /**
     */
    default void repoCheckPackageInstalled(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRepoCheckPackageInstalledMethod(), responseObserver);
    }

    /**
     */
    default void repoRemovePackage(com.intellij.r.psi.rinterop.RepoRemovePackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRepoRemovePackageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    default void previewDataImport(com.intellij.r.psi.rinterop.PreviewDataImportRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPreviewDataImportMethod(), responseObserver);
    }

    /**
     */
    default void commitDataImport(com.intellij.r.psi.rinterop.CommitDataImportRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCommitDataImportMethod(), responseObserver);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    default void copyToPersistentRef(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCopyToPersistentRefMethod(), responseObserver);
    }

    /**
     */
    default void disposePersistentRefs(com.intellij.r.psi.rinterop.PersistentRefList request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDisposePersistentRefsMethod(), responseObserver);
    }

    /**
     */
    default void loaderGetParentEnvs(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ParentEnvsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoaderGetParentEnvsMethod(), responseObserver);
    }

    /**
     */
    default void loaderGetVariables(com.intellij.r.psi.rinterop.GetVariablesRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.VariablesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoaderGetVariablesMethod(), responseObserver);
    }

    /**
     */
    default void loaderGetLoadedNamespaces(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoaderGetLoadedNamespacesMethod(), responseObserver);
    }

    /**
     */
    default void loaderGetValueInfo(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoaderGetValueInfoMethod(), responseObserver);
    }

    /**
     */
    default void evaluateAsText(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringOrError> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEvaluateAsTextMethod(), responseObserver);
    }

    /**
     */
    default void evaluateAsBoolean(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getEvaluateAsBooleanMethod(), responseObserver);
    }

    /**
     */
    default void getDistinctStrings(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDistinctStringsMethod(), responseObserver);
    }

    /**
     */
    default void loadObjectNames(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadObjectNamesMethod(), responseObserver);
    }

    /**
     */
    default void findInheritorNamedArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindInheritorNamedArgumentsMethod(), responseObserver);
    }

    /**
     */
    default void findExtraNamedArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExtraNamedArguments> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindExtraNamedArgumentsMethod(), responseObserver);
    }

    /**
     */
    default void getS4ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetS4ClassInfoByObjectNameMethod(), responseObserver);
    }

    /**
     */
    default void getR6ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetR6ClassInfoByObjectNameMethod(), responseObserver);
    }

    /**
     */
    default void getTableColumnsInfo(com.intellij.r.psi.rinterop.TableColumnsInfoRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.TableColumnsInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTableColumnsInfoMethod(), responseObserver);
    }

    /**
     */
    default void getFormalArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetFormalArgumentsMethod(), responseObserver);
    }

    /**
     */
    default void getEqualityObject(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetEqualityObjectMethod(), responseObserver);
    }

    /**
     */
    default void setValue(com.intellij.r.psi.rinterop.SetValueRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetValueMethod(), responseObserver);
    }

    /**
     */
    default void getObjectSizes(com.intellij.r.psi.rinterop.RRefList request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.Int64List> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetObjectSizesMethod(), responseObserver);
    }

    /**
     */
    default void getRMarkdownChunkOptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRMarkdownChunkOptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    default void dataFrameRegister(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameRegisterMethod(), responseObserver);
    }

    /**
     */
    default void dataFrameGetInfo(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameGetInfoMethod(), responseObserver);
    }

    /**
     */
    default void dataFrameGetData(com.intellij.r.psi.rinterop.DataFrameGetDataRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameGetDataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameGetDataMethod(), responseObserver);
    }

    /**
     */
    default void dataFrameSort(com.intellij.r.psi.rinterop.DataFrameSortRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameSortMethod(), responseObserver);
    }

    /**
     */
    default void dataFrameFilter(com.intellij.r.psi.rinterop.DataFrameFilterRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameFilterMethod(), responseObserver);
    }

    /**
     */
    default void dataFrameRefresh(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDataFrameRefreshMethod(), responseObserver);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    default void convertRoxygenToHTML(com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getConvertRoxygenToHTMLMethod(), responseObserver);
    }

    /**
     */
    default void httpdRequest(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHttpdRequestMethod(), responseObserver);
    }

    /**
     */
    default void getDocumentationForPackage(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDocumentationForPackageMethod(), responseObserver);
    }

    /**
     */
    default void getDocumentationForSymbol(com.intellij.r.psi.rinterop.DocumentationForSymbolRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDocumentationForSymbolMethod(), responseObserver);
    }

    /**
     */
    default void startHttpd(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStartHttpdMethod(), responseObserver);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    default void getWorkingDir(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetWorkingDirMethod(), responseObserver);
    }

    /**
     */
    default void setWorkingDir(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetWorkingDirMethod(), responseObserver);
    }

    /**
     */
    default void clearEnvironment(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getClearEnvironmentMethod(), responseObserver);
    }

    /**
     */
    default void getSysEnv(com.intellij.r.psi.rinterop.GetSysEnvRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSysEnvMethod(), responseObserver);
    }

    /**
     */
    default void loadInstalledPackages(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RInstalledPackageList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadInstalledPackagesMethod(), responseObserver);
    }

    /**
     */
    default void loadLibPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RLibraryPathList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadLibPathsMethod(), responseObserver);
    }

    /**
     */
    default void loadLibrary(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadLibraryMethod(), responseObserver);
    }

    /**
     */
    default void unloadLibrary(com.intellij.r.psi.rinterop.UnloadLibraryRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUnloadLibraryMethod(), responseObserver);
    }

    /**
     */
    default void saveGlobalEnvironment(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSaveGlobalEnvironmentMethod(), responseObserver);
    }

    /**
     */
    default void loadEnvironment(com.intellij.r.psi.rinterop.LoadEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLoadEnvironmentMethod(), responseObserver);
    }

    /**
     */
    default void setOutputWidth(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetOutputWidthMethod(), responseObserver);
    }

    /**
     */
    default void clientRequestFinished(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getClientRequestFinishedMethod(), responseObserver);
    }

    /**
     */
    default void rStudioApiResponse(com.intellij.r.psi.rinterop.RObject request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRStudioApiResponseMethod(), responseObserver);
    }

    /**
     */
    default void setSaveOnExit(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetSaveOnExitMethod(), responseObserver);
    }

    /**
     */
    default void setRStudioApiEnabled(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSetRStudioApiEnabledMethod(), responseObserver);
    }

    /**
     */
    default void getLoadedShortS4ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortS4ClassInfoList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLoadedShortS4ClassInfosMethod(), responseObserver);
    }

    /**
     */
    default void getS4ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetS4ClassInfoByClassNameMethod(), responseObserver);
    }

    /**
     */
    default void getLoadedShortR6ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortR6ClassInfoList> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLoadedShortR6ClassInfosMethod(), responseObserver);
    }

    /**
     */
    default void getR6ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetR6ClassInfoByClassNameMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RPIService.
   */
  public static abstract class RPIServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RPIServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RPIService.
   */
  public static final class RPIServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RPIServiceStub> {
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
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void isBusy(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getIsBusyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void init(com.intellij.r.psi.rinterop.Init request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getInitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void quit(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQuitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void quitProceed(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getQuitProceedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeCode(com.intellij.r.psi.rinterop.ExecuteCodeRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExecuteCodeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getExecuteCodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendReadLn(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendReadLnMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendEof(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendEofMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void replInterrupt(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReplInterruptMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAsyncEvents(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.AsyncEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetAsyncEventsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public void debugAddOrModifyBreakpoint(com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugAddOrModifyBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugSetMasterBreakpoint(com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugSetMasterBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugRemoveBreakpoint(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugRemoveBreakpointMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandContinue(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandContinueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandPause(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandPauseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStop(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandStopMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepOver(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepOverMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepInto(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepIntoMyCode(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMyCodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandStepOut(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandStepOutMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugCommandRunToPosition(com.intellij.r.psi.rinterop.SourcePosition request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugCommandRunToPositionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void debugMuteBreakpoints(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDebugMuteBreakpointsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getFunctionSourcePosition(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetFunctionSourcePositionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSourceFileText(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSourceFileTextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSourceFileName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSourceFileNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    public void graphicsInit(com.intellij.r.psi.rinterop.GraphicsInitRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsInitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsDump(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsDumpResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGraphicsDumpMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRescale(com.intellij.r.psi.rinterop.GraphicsRescaleRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRescaleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRescaleStored(com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRescaleStoredMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsSetParameters(com.intellij.r.psi.rinterop.ScreenParameters request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGraphicsSetParametersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsGetSnapshotPath(com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGraphicsGetSnapshotPathMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsFetchPlot(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGraphicsFetchPlotMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsCreateGroup(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsCreateGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsRemoveGroup(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsRemoveGroupMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void graphicsShutdown(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGraphicsShutdownMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    public void beforeChunkExecution(com.intellij.r.psi.rinterop.ChunkParameters request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getBeforeChunkExecutionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void afterChunkExecution(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getAfterChunkExecutionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pullChunkOutputPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPullChunkOutputPathsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    public void repoGetPackageVersion(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getRepoGetPackageVersionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoInstallPackage(com.intellij.r.psi.rinterop.RepoInstallPackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRepoInstallPackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoAddLibraryPath(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getRepoAddLibraryPathMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoCheckPackageInstalled(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getRepoCheckPackageInstalledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void repoRemovePackage(com.intellij.r.psi.rinterop.RepoRemovePackageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRepoRemovePackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    public void previewDataImport(com.intellij.r.psi.rinterop.PreviewDataImportRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getPreviewDataImportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commitDataImport(com.intellij.r.psi.rinterop.CommitDataImportRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCommitDataImportMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public void copyToPersistentRef(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCopyToPersistentRefMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void disposePersistentRefs(com.intellij.r.psi.rinterop.PersistentRefList request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDisposePersistentRefsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetParentEnvs(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ParentEnvsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoaderGetParentEnvsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetVariables(com.intellij.r.psi.rinterop.GetVariablesRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.VariablesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoaderGetVariablesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetLoadedNamespaces(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoaderGetLoadedNamespacesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loaderGetValueInfo(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoaderGetValueInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void evaluateAsText(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringOrError> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEvaluateAsTextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void evaluateAsBoolean(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getEvaluateAsBooleanMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDistinctStrings(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDistinctStringsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadObjectNames(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadObjectNamesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void findInheritorNamedArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindInheritorNamedArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void findExtraNamedArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExtraNamedArguments> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFindExtraNamedArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByObjectNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByObjectNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTableColumnsInfo(com.intellij.r.psi.rinterop.TableColumnsInfoRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.TableColumnsInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTableColumnsInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getFormalArguments(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetFormalArgumentsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEqualityObject(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetEqualityObjectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setValue(com.intellij.r.psi.rinterop.SetValueRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetValueMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getObjectSizes(com.intellij.r.psi.rinterop.RRefList request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.Int64List> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetObjectSizesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getRMarkdownChunkOptions(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRMarkdownChunkOptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public void dataFrameRegister(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameRegisterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameGetInfo(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameGetInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameGetData(com.intellij.r.psi.rinterop.DataFrameGetDataRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameGetDataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameGetDataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameSort(com.intellij.r.psi.rinterop.DataFrameSortRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameSortMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameFilter(com.intellij.r.psi.rinterop.DataFrameFilterRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameFilterMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dataFrameRefresh(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDataFrameRefreshMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public void convertRoxygenToHTML(com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getConvertRoxygenToHTMLMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void httpdRequest(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHttpdRequestMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDocumentationForPackage(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDocumentationForPackageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDocumentationForSymbol(com.intellij.r.psi.rinterop.DocumentationForSymbolRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDocumentationForSymbolMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void startHttpd(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStartHttpdMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public void getWorkingDir(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.StringValue> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetWorkingDirMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setWorkingDir(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetWorkingDirMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clearEnvironment(com.intellij.r.psi.rinterop.RRef request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getClearEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getSysEnv(com.intellij.r.psi.rinterop.GetSysEnvRequest request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSysEnvMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadInstalledPackages(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RInstalledPackageList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadInstalledPackagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadLibPaths(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RLibraryPathList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadLibPathsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadLibrary(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadLibraryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void unloadLibrary(com.intellij.r.psi.rinterop.UnloadLibraryRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUnloadLibraryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void saveGlobalEnvironment(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSaveGlobalEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void loadEnvironment(com.intellij.r.psi.rinterop.LoadEnvironmentRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLoadEnvironmentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setOutputWidth(com.google.protobuf.Int32Value request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetOutputWidthMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void clientRequestFinished(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getClientRequestFinishedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void rStudioApiResponse(com.intellij.r.psi.rinterop.RObject request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRStudioApiResponseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setSaveOnExit(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSetSaveOnExitMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void setRStudioApiEnabled(com.google.protobuf.BoolValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getSetRStudioApiEnabledMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLoadedShortS4ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortS4ClassInfoList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetLoadedShortS4ClassInfosMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getS4ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByClassNameMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getLoadedShortR6ClassInfos(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortR6ClassInfoList> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetLoadedShortR6ClassInfosMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getR6ClassInfoByClassName(com.google.protobuf.StringValue request,
        io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByClassNameMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RPIService.
   */
  public static final class RPIServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RPIServiceBlockingStub> {
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
    public com.intellij.r.psi.rinterop.GetInfoResponse getInfo(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue isBusy(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getIsBusyMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> init(
        com.intellij.r.psi.rinterop.Init request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getInitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty quit(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQuitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty quitProceed(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getQuitProceedMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.ExecuteCodeResponse> executeCode(
        com.intellij.r.psi.rinterop.ExecuteCodeRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getExecuteCodeMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty sendReadLn(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendReadLnMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty sendEof(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendEofMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty replInterrupt(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReplInterruptMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.AsyncEvent> getAsyncEvents(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetAsyncEventsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public com.google.protobuf.Empty debugAddOrModifyBreakpoint(com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugAddOrModifyBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugSetMasterBreakpoint(com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugSetMasterBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugRemoveBreakpoint(com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugRemoveBreakpointMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandContinue(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandContinueMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandPause(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandPauseMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStop(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandStopMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepOver(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandStepOverMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepInto(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandStepIntoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepIntoMyCode(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandStepIntoMyCodeMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandStepOut(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandStepOutMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugCommandRunToPosition(com.intellij.r.psi.rinterop.SourcePosition request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugCommandRunToPositionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty debugMuteBreakpoints(com.google.protobuf.BoolValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDebugMuteBreakpointsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse getFunctionSourcePosition(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetFunctionSourcePositionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.StringValue getSourceFileText(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSourceFileTextMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.StringValue getSourceFileName(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSourceFileNameMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Graphics device service points
     * </pre>
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsInit(
        com.intellij.r.psi.rinterop.GraphicsInitRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsInitMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.GraphicsDumpResponse graphicsDump(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGraphicsDumpMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsRescale(
        com.intellij.r.psi.rinterop.GraphicsRescaleRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsRescaleMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsRescaleStored(
        com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsRescaleStoredMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty graphicsSetParameters(com.intellij.r.psi.rinterop.ScreenParameters request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGraphicsSetParametersMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse graphicsGetSnapshotPath(com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGraphicsGetSnapshotPathMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse graphicsFetchPlot(com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGraphicsFetchPlotMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsCreateGroup(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsCreateGroupMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsRemoveGroup(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsRemoveGroupMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> graphicsShutdown(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGraphicsShutdownMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * RMarkdown chunks
     * </pre>
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> beforeChunkExecution(
        com.intellij.r.psi.rinterop.ChunkParameters request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getBeforeChunkExecutionMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> afterChunkExecution(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getAfterChunkExecutionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList pullChunkOutputPaths(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPullChunkOutputPathsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Repo utils service points
     * </pre>
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> repoGetPackageVersion(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getRepoGetPackageVersionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty repoInstallPackage(com.intellij.r.psi.rinterop.RepoInstallPackageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRepoInstallPackageMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> repoAddLibraryPath(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getRepoAddLibraryPathMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> repoCheckPackageInstalled(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getRepoCheckPackageInstalledMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty repoRemovePackage(com.intellij.r.psi.rinterop.RepoRemovePackageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRepoRemovePackageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Dataset import service points
     * </pre>
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> previewDataImport(
        com.intellij.r.psi.rinterop.PreviewDataImportRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getPreviewDataImportMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty commitDataImport(com.intellij.r.psi.rinterop.CommitDataImportRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCommitDataImportMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public com.intellij.r.psi.rinterop.CopyToPersistentRefResponse copyToPersistentRef(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCopyToPersistentRefMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty disposePersistentRefs(com.intellij.r.psi.rinterop.PersistentRefList request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDisposePersistentRefsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.ParentEnvsResponse loaderGetParentEnvs(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoaderGetParentEnvsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.VariablesResponse loaderGetVariables(com.intellij.r.psi.rinterop.GetVariablesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoaderGetVariablesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList loaderGetLoadedNamespaces(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoaderGetLoadedNamespacesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.ValueInfo loaderGetValueInfo(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoaderGetValueInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringOrError evaluateAsText(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEvaluateAsTextMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue evaluateAsBoolean(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getEvaluateAsBooleanMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList getDistinctStrings(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDistinctStringsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList loadObjectNames(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadObjectNamesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList findInheritorNamedArguments(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindInheritorNamedArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.ExtraNamedArguments findExtraNamedArguments(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFindExtraNamedArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.S4ClassInfo getS4ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetS4ClassInfoByObjectNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.R6ClassInfo getR6ClassInfoByObjectName(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetR6ClassInfoByObjectNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.TableColumnsInfo getTableColumnsInfo(com.intellij.r.psi.rinterop.TableColumnsInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTableColumnsInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList getFormalArguments(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetFormalArgumentsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int64Value getEqualityObject(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetEqualityObjectMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.ValueInfo setValue(com.intellij.r.psi.rinterop.SetValueRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetValueMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.Int64List getObjectSizes(com.intellij.r.psi.rinterop.RRefList request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetObjectSizesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList getRMarkdownChunkOptions(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRMarkdownChunkOptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public com.google.protobuf.Int32Value dataFrameRegister(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameRegisterMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.DataFrameInfoResponse dataFrameGetInfo(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameGetInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.DataFrameGetDataResponse dataFrameGetData(com.intellij.r.psi.rinterop.DataFrameGetDataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameGetDataMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value dataFrameSort(com.intellij.r.psi.rinterop.DataFrameSortRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameSortMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value dataFrameFilter(com.intellij.r.psi.rinterop.DataFrameFilterRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameFilterMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.BoolValue dataFrameRefresh(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDataFrameRefreshMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse convertRoxygenToHTML(com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getConvertRoxygenToHTMLMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.HttpdResponse httpdRequest(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHttpdRequestMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.HttpdResponse getDocumentationForPackage(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDocumentationForPackageMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.HttpdResponse getDocumentationForSymbol(com.intellij.r.psi.rinterop.DocumentationForSymbolRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDocumentationForSymbolMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Int32Value startHttpd(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStartHttpdMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public com.google.protobuf.StringValue getWorkingDir(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetWorkingDirMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setWorkingDir(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetWorkingDirMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty clearEnvironment(com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getClearEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.StringList getSysEnv(com.intellij.r.psi.rinterop.GetSysEnvRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSysEnvMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.RInstalledPackageList loadInstalledPackages(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadInstalledPackagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.rinterop.RLibraryPathList loadLibPaths(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadLibPathsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty loadLibrary(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadLibraryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty unloadLibrary(com.intellij.r.psi.rinterop.UnloadLibraryRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUnloadLibraryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty saveGlobalEnvironment(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSaveGlobalEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty loadEnvironment(com.intellij.r.psi.rinterop.LoadEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLoadEnvironmentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setOutputWidth(com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetOutputWidthMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty clientRequestFinished(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getClientRequestFinishedMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty rStudioApiResponse(com.intellij.r.psi.rinterop.RObject request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRStudioApiResponseMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty setSaveOnExit(com.google.protobuf.BoolValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSetSaveOnExitMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.intellij.r.psi.rinterop.CommandOutput> setRStudioApiEnabled(
        com.google.protobuf.BoolValue request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getSetRStudioApiEnabledMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.ShortS4ClassInfoList getLoadedShortS4ClassInfos(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetLoadedShortS4ClassInfosMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.S4ClassInfo getS4ClassInfoByClassName(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetS4ClassInfoByClassNameMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.ShortR6ClassInfoList getLoadedShortR6ClassInfos(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetLoadedShortR6ClassInfosMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.intellij.r.psi.classes.R6ClassInfo getR6ClassInfoByClassName(com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetR6ClassInfoByClassNameMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RPIService.
   */
  public static final class RPIServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RPIServiceFutureStub> {
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
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.GetInfoResponse> getInfo(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> isBusy(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getIsBusyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> quit(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQuitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> quitProceed(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getQuitProceedMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> sendReadLn(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendReadLnMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> sendEof(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendEofMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> replInterrupt(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReplInterruptMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Debugger
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugAddOrModifyBreakpoint(
        com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugAddOrModifyBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugSetMasterBreakpoint(
        com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugSetMasterBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugRemoveBreakpoint(
        com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugRemoveBreakpointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandContinue(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandContinueMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandPause(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandPauseMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStop(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandStopMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepOver(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandStepOverMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepInto(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepIntoMyCode(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandStepIntoMyCodeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandStepOut(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandStepOutMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugCommandRunToPosition(
        com.intellij.r.psi.rinterop.SourcePosition request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugCommandRunToPositionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> debugMuteBreakpoints(
        com.google.protobuf.BoolValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDebugMuteBreakpointsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse> getFunctionSourcePosition(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetFunctionSourcePositionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getSourceFileText(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSourceFileTextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getSourceFileName(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSourceFileNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.GraphicsDumpResponse> graphicsDump(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGraphicsDumpMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> graphicsSetParameters(
        com.intellij.r.psi.rinterop.ScreenParameters request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGraphicsSetParametersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse> graphicsGetSnapshotPath(
        com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGraphicsGetSnapshotPathMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse> graphicsFetchPlot(
        com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGraphicsFetchPlotMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> pullChunkOutputPaths(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPullChunkOutputPathsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> repoInstallPackage(
        com.intellij.r.psi.rinterop.RepoInstallPackageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRepoInstallPackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> repoRemovePackage(
        com.intellij.r.psi.rinterop.RepoRemovePackageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRepoRemovePackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> commitDataImport(
        com.intellij.r.psi.rinterop.CommitDataImportRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCommitDataImportMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Methods for RRef and RVariableLoader
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.CopyToPersistentRefResponse> copyToPersistentRef(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCopyToPersistentRefMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> disposePersistentRefs(
        com.intellij.r.psi.rinterop.PersistentRefList request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDisposePersistentRefsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.ParentEnvsResponse> loaderGetParentEnvs(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoaderGetParentEnvsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.VariablesResponse> loaderGetVariables(
        com.intellij.r.psi.rinterop.GetVariablesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoaderGetVariablesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> loaderGetLoadedNamespaces(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoaderGetLoadedNamespacesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.ValueInfo> loaderGetValueInfo(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoaderGetValueInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringOrError> evaluateAsText(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEvaluateAsTextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> evaluateAsBoolean(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getEvaluateAsBooleanMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> getDistinctStrings(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDistinctStringsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> loadObjectNames(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadObjectNamesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> findInheritorNamedArguments(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindInheritorNamedArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.ExtraNamedArguments> findExtraNamedArguments(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFindExtraNamedArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.S4ClassInfo> getS4ClassInfoByObjectName(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByObjectNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.R6ClassInfo> getR6ClassInfoByObjectName(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetR6ClassInfoByObjectNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.TableColumnsInfo> getTableColumnsInfo(
        com.intellij.r.psi.rinterop.TableColumnsInfoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTableColumnsInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> getFormalArguments(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetFormalArgumentsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int64Value> getEqualityObject(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetEqualityObjectMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.ValueInfo> setValue(
        com.intellij.r.psi.rinterop.SetValueRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetValueMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.Int64List> getObjectSizes(
        com.intellij.r.psi.rinterop.RRefList request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetObjectSizesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> getRMarkdownChunkOptions(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRMarkdownChunkOptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Data frame viewer
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameRegister(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameRegisterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.DataFrameInfoResponse> dataFrameGetInfo(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameGetInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.DataFrameGetDataResponse> dataFrameGetData(
        com.intellij.r.psi.rinterop.DataFrameGetDataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameGetDataMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameSort(
        com.intellij.r.psi.rinterop.DataFrameSortRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameSortMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> dataFrameFilter(
        com.intellij.r.psi.rinterop.DataFrameFilterRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameFilterMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.BoolValue> dataFrameRefresh(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDataFrameRefreshMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Documentation and http
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse> convertRoxygenToHTML(
        com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getConvertRoxygenToHTMLMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.HttpdResponse> httpdRequest(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHttpdRequestMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.HttpdResponse> getDocumentationForPackage(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDocumentationForPackageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.HttpdResponse> getDocumentationForSymbol(
        com.intellij.r.psi.rinterop.DocumentationForSymbolRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDocumentationForSymbolMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Int32Value> startHttpd(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStartHttpdMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Misc
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.StringValue> getWorkingDir(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetWorkingDirMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setWorkingDir(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetWorkingDirMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> clearEnvironment(
        com.intellij.r.psi.rinterop.RRef request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getClearEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.StringList> getSysEnv(
        com.intellij.r.psi.rinterop.GetSysEnvRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSysEnvMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.RInstalledPackageList> loadInstalledPackages(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadInstalledPackagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.rinterop.RLibraryPathList> loadLibPaths(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadLibPathsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> loadLibrary(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadLibraryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> unloadLibrary(
        com.intellij.r.psi.rinterop.UnloadLibraryRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUnloadLibraryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> saveGlobalEnvironment(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSaveGlobalEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> loadEnvironment(
        com.intellij.r.psi.rinterop.LoadEnvironmentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLoadEnvironmentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setOutputWidth(
        com.google.protobuf.Int32Value request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetOutputWidthMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> clientRequestFinished(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getClientRequestFinishedMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> rStudioApiResponse(
        com.intellij.r.psi.rinterop.RObject request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRStudioApiResponseMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> setSaveOnExit(
        com.google.protobuf.BoolValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSetSaveOnExitMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.ShortS4ClassInfoList> getLoadedShortS4ClassInfos(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetLoadedShortS4ClassInfosMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.S4ClassInfo> getS4ClassInfoByClassName(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetS4ClassInfoByClassNameMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.ShortR6ClassInfoList> getLoadedShortR6ClassInfos(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetLoadedShortR6ClassInfosMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.intellij.r.psi.classes.R6ClassInfo> getR6ClassInfoByClassName(
        com.google.protobuf.StringValue request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
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
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_INFO:
          serviceImpl.getInfo((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetInfoResponse>) responseObserver);
          break;
        case METHODID_IS_BUSY:
          serviceImpl.isBusy((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_INIT:
          serviceImpl.init((com.intellij.r.psi.rinterop.Init) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
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
          serviceImpl.executeCode((com.intellij.r.psi.rinterop.ExecuteCodeRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExecuteCodeResponse>) responseObserver);
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
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.AsyncEvent>) responseObserver);
          break;
        case METHODID_DEBUG_ADD_OR_MODIFY_BREAKPOINT:
          serviceImpl.debugAddOrModifyBreakpoint((com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_SET_MASTER_BREAKPOINT:
          serviceImpl.debugSetMasterBreakpoint((com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest) request,
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
          serviceImpl.debugCommandRunToPosition((com.intellij.r.psi.rinterop.SourcePosition) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DEBUG_MUTE_BREAKPOINTS:
          serviceImpl.debugMuteBreakpoints((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_FUNCTION_SOURCE_POSITION:
          serviceImpl.getFunctionSourcePosition((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse>) responseObserver);
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
          serviceImpl.graphicsInit((com.intellij.r.psi.rinterop.GraphicsInitRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_DUMP:
          serviceImpl.graphicsDump((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsDumpResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_RESCALE:
          serviceImpl.graphicsRescale((com.intellij.r.psi.rinterop.GraphicsRescaleRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_RESCALE_STORED:
          serviceImpl.graphicsRescaleStored((com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_SET_PARAMETERS:
          serviceImpl.graphicsSetParameters((com.intellij.r.psi.rinterop.ScreenParameters) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GRAPHICS_GET_SNAPSHOT_PATH:
          serviceImpl.graphicsGetSnapshotPath((com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_FETCH_PLOT:
          serviceImpl.graphicsFetchPlot((com.google.protobuf.Int32Value) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse>) responseObserver);
          break;
        case METHODID_GRAPHICS_CREATE_GROUP:
          serviceImpl.graphicsCreateGroup((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_REMOVE_GROUP:
          serviceImpl.graphicsRemoveGroup((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GRAPHICS_SHUTDOWN:
          serviceImpl.graphicsShutdown((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_BEFORE_CHUNK_EXECUTION:
          serviceImpl.beforeChunkExecution((com.intellij.r.psi.rinterop.ChunkParameters) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_AFTER_CHUNK_EXECUTION:
          serviceImpl.afterChunkExecution((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_PULL_CHUNK_OUTPUT_PATHS:
          serviceImpl.pullChunkOutputPaths((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_REPO_GET_PACKAGE_VERSION:
          serviceImpl.repoGetPackageVersion((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_INSTALL_PACKAGE:
          serviceImpl.repoInstallPackage((com.intellij.r.psi.rinterop.RepoInstallPackageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_REPO_ADD_LIBRARY_PATH:
          serviceImpl.repoAddLibraryPath((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_CHECK_PACKAGE_INSTALLED:
          serviceImpl.repoCheckPackageInstalled((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_REPO_REMOVE_PACKAGE:
          serviceImpl.repoRemovePackage((com.intellij.r.psi.rinterop.RepoRemovePackageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_PREVIEW_DATA_IMPORT:
          serviceImpl.previewDataImport((com.intellij.r.psi.rinterop.PreviewDataImportRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_COMMIT_DATA_IMPORT:
          serviceImpl.commitDataImport((com.intellij.r.psi.rinterop.CommitDataImportRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_COPY_TO_PERSISTENT_REF:
          serviceImpl.copyToPersistentRef((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CopyToPersistentRefResponse>) responseObserver);
          break;
        case METHODID_DISPOSE_PERSISTENT_REFS:
          serviceImpl.disposePersistentRefs((com.intellij.r.psi.rinterop.PersistentRefList) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LOADER_GET_PARENT_ENVS:
          serviceImpl.loaderGetParentEnvs((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ParentEnvsResponse>) responseObserver);
          break;
        case METHODID_LOADER_GET_VARIABLES:
          serviceImpl.loaderGetVariables((com.intellij.r.psi.rinterop.GetVariablesRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.VariablesResponse>) responseObserver);
          break;
        case METHODID_LOADER_GET_LOADED_NAMESPACES:
          serviceImpl.loaderGetLoadedNamespaces((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOADER_GET_VALUE_INFO:
          serviceImpl.loaderGetValueInfo((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo>) responseObserver);
          break;
        case METHODID_EVALUATE_AS_TEXT:
          serviceImpl.evaluateAsText((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringOrError>) responseObserver);
          break;
        case METHODID_EVALUATE_AS_BOOLEAN:
          serviceImpl.evaluateAsBoolean((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_GET_DISTINCT_STRINGS:
          serviceImpl.getDistinctStrings((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOAD_OBJECT_NAMES:
          serviceImpl.loadObjectNames((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_FIND_INHERITOR_NAMED_ARGUMENTS:
          serviceImpl.findInheritorNamedArguments((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_FIND_EXTRA_NAMED_ARGUMENTS:
          serviceImpl.findExtraNamedArguments((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ExtraNamedArguments>) responseObserver);
          break;
        case METHODID_GET_S4CLASS_INFO_BY_OBJECT_NAME:
          serviceImpl.getS4ClassInfoByObjectName((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo>) responseObserver);
          break;
        case METHODID_GET_R6CLASS_INFO_BY_OBJECT_NAME:
          serviceImpl.getR6ClassInfoByObjectName((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo>) responseObserver);
          break;
        case METHODID_GET_TABLE_COLUMNS_INFO:
          serviceImpl.getTableColumnsInfo((com.intellij.r.psi.rinterop.TableColumnsInfoRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.TableColumnsInfo>) responseObserver);
          break;
        case METHODID_GET_FORMAL_ARGUMENTS:
          serviceImpl.getFormalArguments((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_GET_EQUALITY_OBJECT:
          serviceImpl.getEqualityObject((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int64Value>) responseObserver);
          break;
        case METHODID_SET_VALUE:
          serviceImpl.setValue((com.intellij.r.psi.rinterop.SetValueRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ValueInfo>) responseObserver);
          break;
        case METHODID_GET_OBJECT_SIZES:
          serviceImpl.getObjectSizes((com.intellij.r.psi.rinterop.RRefList) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.Int64List>) responseObserver);
          break;
        case METHODID_GET_RMARKDOWN_CHUNK_OPTIONS:
          serviceImpl.getRMarkdownChunkOptions((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_DATA_FRAME_REGISTER:
          serviceImpl.dataFrameRegister((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_GET_INFO:
          serviceImpl.dataFrameGetInfo((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameInfoResponse>) responseObserver);
          break;
        case METHODID_DATA_FRAME_GET_DATA:
          serviceImpl.dataFrameGetData((com.intellij.r.psi.rinterop.DataFrameGetDataRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.DataFrameGetDataResponse>) responseObserver);
          break;
        case METHODID_DATA_FRAME_SORT:
          serviceImpl.dataFrameSort((com.intellij.r.psi.rinterop.DataFrameSortRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_FILTER:
          serviceImpl.dataFrameFilter((com.intellij.r.psi.rinterop.DataFrameFilterRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Int32Value>) responseObserver);
          break;
        case METHODID_DATA_FRAME_REFRESH:
          serviceImpl.dataFrameRefresh((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue>) responseObserver);
          break;
        case METHODID_CONVERT_ROXYGEN_TO_HTML:
          serviceImpl.convertRoxygenToHTML((com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse>) responseObserver);
          break;
        case METHODID_HTTPD_REQUEST:
          serviceImpl.httpdRequest((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse>) responseObserver);
          break;
        case METHODID_GET_DOCUMENTATION_FOR_PACKAGE:
          serviceImpl.getDocumentationForPackage((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse>) responseObserver);
          break;
        case METHODID_GET_DOCUMENTATION_FOR_SYMBOL:
          serviceImpl.getDocumentationForSymbol((com.intellij.r.psi.rinterop.DocumentationForSymbolRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.HttpdResponse>) responseObserver);
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
          serviceImpl.clearEnvironment((com.intellij.r.psi.rinterop.RRef) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_GET_SYS_ENV:
          serviceImpl.getSysEnv((com.intellij.r.psi.rinterop.GetSysEnvRequest) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.StringList>) responseObserver);
          break;
        case METHODID_LOAD_INSTALLED_PACKAGES:
          serviceImpl.loadInstalledPackages((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RInstalledPackageList>) responseObserver);
          break;
        case METHODID_LOAD_LIB_PATHS:
          serviceImpl.loadLibPaths((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.RLibraryPathList>) responseObserver);
          break;
        case METHODID_LOAD_LIBRARY:
          serviceImpl.loadLibrary((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_UNLOAD_LIBRARY:
          serviceImpl.unloadLibrary((com.intellij.r.psi.rinterop.UnloadLibraryRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SAVE_GLOBAL_ENVIRONMENT:
          serviceImpl.saveGlobalEnvironment((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LOAD_ENVIRONMENT:
          serviceImpl.loadEnvironment((com.intellij.r.psi.rinterop.LoadEnvironmentRequest) request,
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
          serviceImpl.rStudioApiResponse((com.intellij.r.psi.rinterop.RObject) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_SAVE_ON_EXIT:
          serviceImpl.setSaveOnExit((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_SET_RSTUDIO_API_ENABLED:
          serviceImpl.setRStudioApiEnabled((com.google.protobuf.BoolValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.rinterop.CommandOutput>) responseObserver);
          break;
        case METHODID_GET_LOADED_SHORT_S4CLASS_INFOS:
          serviceImpl.getLoadedShortS4ClassInfos((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortS4ClassInfoList>) responseObserver);
          break;
        case METHODID_GET_S4CLASS_INFO_BY_CLASS_NAME:
          serviceImpl.getS4ClassInfoByClassName((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.S4ClassInfo>) responseObserver);
          break;
        case METHODID_GET_LOADED_SHORT_R6CLASS_INFOS:
          serviceImpl.getLoadedShortR6ClassInfos((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.ShortR6ClassInfoList>) responseObserver);
          break;
        case METHODID_GET_R6CLASS_INFO_BY_CLASS_NAME:
          serviceImpl.getR6ClassInfoByClassName((com.google.protobuf.StringValue) request,
              (io.grpc.stub.StreamObserver<com.intellij.r.psi.classes.R6ClassInfo>) responseObserver);
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

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.GetInfoResponse>(
                service, METHODID_GET_INFO)))
        .addMethod(
          getIsBusyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.BoolValue>(
                service, METHODID_IS_BUSY)))
        .addMethod(
          getInitMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.Init,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_INIT)))
        .addMethod(
          getQuitMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_QUIT)))
        .addMethod(
          getQuitProceedMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_QUIT_PROCEED)))
        .addMethod(
          getExecuteCodeMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.ExecuteCodeRequest,
              com.intellij.r.psi.rinterop.ExecuteCodeResponse>(
                service, METHODID_EXECUTE_CODE)))
        .addMethod(
          getSendReadLnMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.Empty>(
                service, METHODID_SEND_READ_LN)))
        .addMethod(
          getSendEofMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_SEND_EOF)))
        .addMethod(
          getReplInterruptMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_REPL_INTERRUPT)))
        .addMethod(
          getGetAsyncEventsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.AsyncEvent>(
                service, METHODID_GET_ASYNC_EVENTS)))
        .addMethod(
          getDebugAddOrModifyBreakpointMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DebugAddOrModifyBreakpointRequest,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_ADD_OR_MODIFY_BREAKPOINT)))
        .addMethod(
          getDebugSetMasterBreakpointMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DebugSetMasterBreakpointRequest,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_SET_MASTER_BREAKPOINT)))
        .addMethod(
          getDebugRemoveBreakpointMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Int32Value,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_REMOVE_BREAKPOINT)))
        .addMethod(
          getDebugCommandContinueMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_CONTINUE)))
        .addMethod(
          getDebugCommandPauseMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_PAUSE)))
        .addMethod(
          getDebugCommandStopMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_STOP)))
        .addMethod(
          getDebugCommandStepOverMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_STEP_OVER)))
        .addMethod(
          getDebugCommandStepIntoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_STEP_INTO)))
        .addMethod(
          getDebugCommandStepIntoMyCodeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_STEP_INTO_MY_CODE)))
        .addMethod(
          getDebugCommandStepOutMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_STEP_OUT)))
        .addMethod(
          getDebugCommandRunToPositionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.SourcePosition,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_COMMAND_RUN_TO_POSITION)))
        .addMethod(
          getDebugMuteBreakpointsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.BoolValue,
              com.google.protobuf.Empty>(
                service, METHODID_DEBUG_MUTE_BREAKPOINTS)))
        .addMethod(
          getGetFunctionSourcePositionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.GetFunctionSourcePositionResponse>(
                service, METHODID_GET_FUNCTION_SOURCE_POSITION)))
        .addMethod(
          getGetSourceFileTextMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.StringValue>(
                service, METHODID_GET_SOURCE_FILE_TEXT)))
        .addMethod(
          getGetSourceFileNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.StringValue>(
                service, METHODID_GET_SOURCE_FILE_NAME)))
        .addMethod(
          getGraphicsInitMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GraphicsInitRequest,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_INIT)))
        .addMethod(
          getGraphicsDumpMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.GraphicsDumpResponse>(
                service, METHODID_GRAPHICS_DUMP)))
        .addMethod(
          getGraphicsRescaleMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GraphicsRescaleRequest,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_RESCALE)))
        .addMethod(
          getGraphicsRescaleStoredMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GraphicsRescaleStoredRequest,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_RESCALE_STORED)))
        .addMethod(
          getGraphicsSetParametersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.ScreenParameters,
              com.google.protobuf.Empty>(
                service, METHODID_GRAPHICS_SET_PARAMETERS)))
        .addMethod(
          getGraphicsGetSnapshotPathMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathRequest,
              com.intellij.r.psi.rinterop.GraphicsGetSnapshotPathResponse>(
                service, METHODID_GRAPHICS_GET_SNAPSHOT_PATH)))
        .addMethod(
          getGraphicsFetchPlotMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Int32Value,
              com.intellij.r.psi.rinterop.GraphicsFetchPlotResponse>(
                service, METHODID_GRAPHICS_FETCH_PLOT)))
        .addMethod(
          getGraphicsCreateGroupMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_CREATE_GROUP)))
        .addMethod(
          getGraphicsRemoveGroupMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_REMOVE_GROUP)))
        .addMethod(
          getGraphicsShutdownMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_GRAPHICS_SHUTDOWN)))
        .addMethod(
          getBeforeChunkExecutionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.ChunkParameters,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_BEFORE_CHUNK_EXECUTION)))
        .addMethod(
          getAfterChunkExecutionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_AFTER_CHUNK_EXECUTION)))
        .addMethod(
          getPullChunkOutputPathsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_PULL_CHUNK_OUTPUT_PATHS)))
        .addMethod(
          getRepoGetPackageVersionMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_REPO_GET_PACKAGE_VERSION)))
        .addMethod(
          getRepoInstallPackageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RepoInstallPackageRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REPO_INSTALL_PACKAGE)))
        .addMethod(
          getRepoAddLibraryPathMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_REPO_ADD_LIBRARY_PATH)))
        .addMethod(
          getRepoCheckPackageInstalledMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_REPO_CHECK_PACKAGE_INSTALLED)))
        .addMethod(
          getRepoRemovePackageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RepoRemovePackageRequest,
              com.google.protobuf.Empty>(
                service, METHODID_REPO_REMOVE_PACKAGE)))
        .addMethod(
          getPreviewDataImportMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.PreviewDataImportRequest,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_PREVIEW_DATA_IMPORT)))
        .addMethod(
          getCommitDataImportMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.CommitDataImportRequest,
              com.google.protobuf.Empty>(
                service, METHODID_COMMIT_DATA_IMPORT)))
        .addMethod(
          getCopyToPersistentRefMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.CopyToPersistentRefResponse>(
                service, METHODID_COPY_TO_PERSISTENT_REF)))
        .addMethod(
          getDisposePersistentRefsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.PersistentRefList,
              com.google.protobuf.Empty>(
                service, METHODID_DISPOSE_PERSISTENT_REFS)))
        .addMethod(
          getLoaderGetParentEnvsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.ParentEnvsResponse>(
                service, METHODID_LOADER_GET_PARENT_ENVS)))
        .addMethod(
          getLoaderGetVariablesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GetVariablesRequest,
              com.intellij.r.psi.rinterop.VariablesResponse>(
                service, METHODID_LOADER_GET_VARIABLES)))
        .addMethod(
          getLoaderGetLoadedNamespacesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_LOADER_GET_LOADED_NAMESPACES)))
        .addMethod(
          getLoaderGetValueInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.ValueInfo>(
                service, METHODID_LOADER_GET_VALUE_INFO)))
        .addMethod(
          getEvaluateAsTextMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.StringOrError>(
                service, METHODID_EVALUATE_AS_TEXT)))
        .addMethod(
          getEvaluateAsBooleanMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.google.protobuf.BoolValue>(
                service, METHODID_EVALUATE_AS_BOOLEAN)))
        .addMethod(
          getGetDistinctStringsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_GET_DISTINCT_STRINGS)))
        .addMethod(
          getLoadObjectNamesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_LOAD_OBJECT_NAMES)))
        .addMethod(
          getFindInheritorNamedArgumentsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_FIND_INHERITOR_NAMED_ARGUMENTS)))
        .addMethod(
          getFindExtraNamedArgumentsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.ExtraNamedArguments>(
                service, METHODID_FIND_EXTRA_NAMED_ARGUMENTS)))
        .addMethod(
          getGetS4ClassInfoByObjectNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.classes.S4ClassInfo>(
                service, METHODID_GET_S4CLASS_INFO_BY_OBJECT_NAME)))
        .addMethod(
          getGetR6ClassInfoByObjectNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.classes.R6ClassInfo>(
                service, METHODID_GET_R6CLASS_INFO_BY_OBJECT_NAME)))
        .addMethod(
          getGetTableColumnsInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.TableColumnsInfoRequest,
              com.intellij.r.psi.rinterop.TableColumnsInfo>(
                service, METHODID_GET_TABLE_COLUMNS_INFO)))
        .addMethod(
          getGetFormalArgumentsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_GET_FORMAL_ARGUMENTS)))
        .addMethod(
          getGetEqualityObjectMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.google.protobuf.Int64Value>(
                service, METHODID_GET_EQUALITY_OBJECT)))
        .addMethod(
          getSetValueMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.SetValueRequest,
              com.intellij.r.psi.rinterop.ValueInfo>(
                service, METHODID_SET_VALUE)))
        .addMethod(
          getGetObjectSizesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRefList,
              com.intellij.r.psi.rinterop.Int64List>(
                service, METHODID_GET_OBJECT_SIZES)))
        .addMethod(
          getGetRMarkdownChunkOptionsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_GET_RMARKDOWN_CHUNK_OPTIONS)))
        .addMethod(
          getDataFrameRegisterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.google.protobuf.Int32Value>(
                service, METHODID_DATA_FRAME_REGISTER)))
        .addMethod(
          getDataFrameGetInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.intellij.r.psi.rinterop.DataFrameInfoResponse>(
                service, METHODID_DATA_FRAME_GET_INFO)))
        .addMethod(
          getDataFrameGetDataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DataFrameGetDataRequest,
              com.intellij.r.psi.rinterop.DataFrameGetDataResponse>(
                service, METHODID_DATA_FRAME_GET_DATA)))
        .addMethod(
          getDataFrameSortMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DataFrameSortRequest,
              com.google.protobuf.Int32Value>(
                service, METHODID_DATA_FRAME_SORT)))
        .addMethod(
          getDataFrameFilterMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DataFrameFilterRequest,
              com.google.protobuf.Int32Value>(
                service, METHODID_DATA_FRAME_FILTER)))
        .addMethod(
          getDataFrameRefreshMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.google.protobuf.BoolValue>(
                service, METHODID_DATA_FRAME_REFRESH)))
        .addMethod(
          getConvertRoxygenToHTMLMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLRequest,
              com.intellij.r.psi.rinterop.ConvertRoxygenToHTMLResponse>(
                service, METHODID_CONVERT_ROXYGEN_TO_HTML)))
        .addMethod(
          getHttpdRequestMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.HttpdResponse>(
                service, METHODID_HTTPD_REQUEST)))
        .addMethod(
          getGetDocumentationForPackageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.rinterop.HttpdResponse>(
                service, METHODID_GET_DOCUMENTATION_FOR_PACKAGE)))
        .addMethod(
          getGetDocumentationForSymbolMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.DocumentationForSymbolRequest,
              com.intellij.r.psi.rinterop.HttpdResponse>(
                service, METHODID_GET_DOCUMENTATION_FOR_SYMBOL)))
        .addMethod(
          getStartHttpdMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Int32Value>(
                service, METHODID_START_HTTPD)))
        .addMethod(
          getGetWorkingDirMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.StringValue>(
                service, METHODID_GET_WORKING_DIR)))
        .addMethod(
          getSetWorkingDirMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.Empty>(
                service, METHODID_SET_WORKING_DIR)))
        .addMethod(
          getClearEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RRef,
              com.google.protobuf.Empty>(
                service, METHODID_CLEAR_ENVIRONMENT)))
        .addMethod(
          getGetSysEnvMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.GetSysEnvRequest,
              com.intellij.r.psi.rinterop.StringList>(
                service, METHODID_GET_SYS_ENV)))
        .addMethod(
          getLoadInstalledPackagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.RInstalledPackageList>(
                service, METHODID_LOAD_INSTALLED_PACKAGES)))
        .addMethod(
          getLoadLibPathsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.rinterop.RLibraryPathList>(
                service, METHODID_LOAD_LIB_PATHS)))
        .addMethod(
          getLoadLibraryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.Empty>(
                service, METHODID_LOAD_LIBRARY)))
        .addMethod(
          getUnloadLibraryMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.UnloadLibraryRequest,
              com.google.protobuf.Empty>(
                service, METHODID_UNLOAD_LIBRARY)))
        .addMethod(
          getSaveGlobalEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.google.protobuf.Empty>(
                service, METHODID_SAVE_GLOBAL_ENVIRONMENT)))
        .addMethod(
          getLoadEnvironmentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.LoadEnvironmentRequest,
              com.google.protobuf.Empty>(
                service, METHODID_LOAD_ENVIRONMENT)))
        .addMethod(
          getSetOutputWidthMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Int32Value,
              com.google.protobuf.Empty>(
                service, METHODID_SET_OUTPUT_WIDTH)))
        .addMethod(
          getClientRequestFinishedMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.google.protobuf.Empty>(
                service, METHODID_CLIENT_REQUEST_FINISHED)))
        .addMethod(
          getRStudioApiResponseMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.intellij.r.psi.rinterop.RObject,
              com.google.protobuf.Empty>(
                service, METHODID_R_STUDIO_API_RESPONSE)))
        .addMethod(
          getSetSaveOnExitMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.BoolValue,
              com.google.protobuf.Empty>(
                service, METHODID_SET_SAVE_ON_EXIT)))
        .addMethod(
          getSetRStudioApiEnabledMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.google.protobuf.BoolValue,
              com.intellij.r.psi.rinterop.CommandOutput>(
                service, METHODID_SET_RSTUDIO_API_ENABLED)))
        .addMethod(
          getGetLoadedShortS4ClassInfosMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.classes.ShortS4ClassInfoList>(
                service, METHODID_GET_LOADED_SHORT_S4CLASS_INFOS)))
        .addMethod(
          getGetS4ClassInfoByClassNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.classes.S4ClassInfo>(
                service, METHODID_GET_S4CLASS_INFO_BY_CLASS_NAME)))
        .addMethod(
          getGetLoadedShortR6ClassInfosMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.intellij.r.psi.classes.ShortR6ClassInfoList>(
                service, METHODID_GET_LOADED_SHORT_R6CLASS_INFOS)))
        .addMethod(
          getGetR6ClassInfoByClassNameMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.StringValue,
              com.intellij.r.psi.classes.R6ClassInfo>(
                service, METHODID_GET_R6CLASS_INFO_BY_CLASS_NAME)))
        .build();
  }

  private static abstract class RPIServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RPIServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.intellij.r.psi.rinterop.Service.getDescriptor();
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
    private final java.lang.String methodName;

    RPIServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
