// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

/**
 * Protobuf type {@code rplugininterop.StackFrame}
 */
public final class StackFrame extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rplugininterop.StackFrame)
    StackFrameOrBuilder {
private static final long serialVersionUID = 0L;
  // Use StackFrame.newBuilder() to construct.
  private StackFrame(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private StackFrame() {
    functionName_ = "";
    sourcePositionText_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new StackFrame();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private StackFrame(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            org.jetbrains.r.rinterop.SourcePosition.Builder subBuilder = null;
            if (position_ != null) {
              subBuilder = position_.toBuilder();
            }
            position_ = input.readMessage(org.jetbrains.r.rinterop.SourcePosition.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(position_);
              position_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            functionName_ = s;
            break;
          }
          case 24: {

            equalityObject_ = input.readInt64();
            break;
          }
          case 34: {
            org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder subBuilder = null;
            if (extendedSourcePosition_ != null) {
              subBuilder = extendedSourcePosition_.toBuilder();
            }
            extendedSourcePosition_ = input.readMessage(org.jetbrains.r.rinterop.ExtendedSourcePosition.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(extendedSourcePosition_);
              extendedSourcePosition_ = subBuilder.buildPartial();
            }

            break;
          }
          case 42: {
            java.lang.String s = input.readStringRequireUtf8();

            sourcePositionText_ = s;
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_StackFrame_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_StackFrame_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.jetbrains.r.rinterop.StackFrame.class, org.jetbrains.r.rinterop.StackFrame.Builder.class);
  }

  public static final int POSITION_FIELD_NUMBER = 1;
  private org.jetbrains.r.rinterop.SourcePosition position_;
  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   * @return Whether the position field is set.
   */
  @java.lang.Override
  public boolean hasPosition() {
    return position_ != null;
  }
  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   * @return The position.
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.SourcePosition getPosition() {
    return position_ == null ? org.jetbrains.r.rinterop.SourcePosition.getDefaultInstance() : position_;
  }
  /**
   * <code>.rplugininterop.SourcePosition position = 1;</code>
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.SourcePositionOrBuilder getPositionOrBuilder() {
    return getPosition();
  }

  public static final int FUNCTIONNAME_FIELD_NUMBER = 2;
  private volatile java.lang.Object functionName_;
  /**
   * <code>string functionName = 2;</code>
   * @return The functionName.
   */
  @java.lang.Override
  public java.lang.String getFunctionName() {
    java.lang.Object ref = functionName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      functionName_ = s;
      return s;
    }
  }
  /**
   * <code>string functionName = 2;</code>
   * @return The bytes for functionName.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getFunctionNameBytes() {
    java.lang.Object ref = functionName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      functionName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int EQUALITYOBJECT_FIELD_NUMBER = 3;
  private long equalityObject_;
  /**
   * <code>int64 equalityObject = 3;</code>
   * @return The equalityObject.
   */
  @java.lang.Override
  public long getEqualityObject() {
    return equalityObject_;
  }

  public static final int EXTENDEDSOURCEPOSITION_FIELD_NUMBER = 4;
  private org.jetbrains.r.rinterop.ExtendedSourcePosition extendedSourcePosition_;
  /**
   * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
   * @return Whether the extendedSourcePosition field is set.
   */
  @java.lang.Override
  public boolean hasExtendedSourcePosition() {
    return extendedSourcePosition_ != null;
  }
  /**
   * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
   * @return The extendedSourcePosition.
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.ExtendedSourcePosition getExtendedSourcePosition() {
    return extendedSourcePosition_ == null ? org.jetbrains.r.rinterop.ExtendedSourcePosition.getDefaultInstance() : extendedSourcePosition_;
  }
  /**
   * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.ExtendedSourcePositionOrBuilder getExtendedSourcePositionOrBuilder() {
    return getExtendedSourcePosition();
  }

  public static final int SOURCEPOSITIONTEXT_FIELD_NUMBER = 5;
  private volatile java.lang.Object sourcePositionText_;
  /**
   * <code>string sourcePositionText = 5;</code>
   * @return The sourcePositionText.
   */
  @java.lang.Override
  public java.lang.String getSourcePositionText() {
    java.lang.Object ref = sourcePositionText_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      sourcePositionText_ = s;
      return s;
    }
  }
  /**
   * <code>string sourcePositionText = 5;</code>
   * @return The bytes for sourcePositionText.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSourcePositionTextBytes() {
    java.lang.Object ref = sourcePositionText_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      sourcePositionText_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (position_ != null) {
      output.writeMessage(1, getPosition());
    }
    if (!getFunctionNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, functionName_);
    }
    if (equalityObject_ != 0L) {
      output.writeInt64(3, equalityObject_);
    }
    if (extendedSourcePosition_ != null) {
      output.writeMessage(4, getExtendedSourcePosition());
    }
    if (!getSourcePositionTextBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 5, sourcePositionText_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (position_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getPosition());
    }
    if (!getFunctionNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, functionName_);
    }
    if (equalityObject_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt64Size(3, equalityObject_);
    }
    if (extendedSourcePosition_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(4, getExtendedSourcePosition());
    }
    if (!getSourcePositionTextBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, sourcePositionText_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof org.jetbrains.r.rinterop.StackFrame)) {
      return super.equals(obj);
    }
    org.jetbrains.r.rinterop.StackFrame other = (org.jetbrains.r.rinterop.StackFrame) obj;

    if (hasPosition() != other.hasPosition()) return false;
    if (hasPosition()) {
      if (!getPosition()
          .equals(other.getPosition())) return false;
    }
    if (!getFunctionName()
        .equals(other.getFunctionName())) return false;
    if (getEqualityObject()
        != other.getEqualityObject()) return false;
    if (hasExtendedSourcePosition() != other.hasExtendedSourcePosition()) return false;
    if (hasExtendedSourcePosition()) {
      if (!getExtendedSourcePosition()
          .equals(other.getExtendedSourcePosition())) return false;
    }
    if (!getSourcePositionText()
        .equals(other.getSourcePositionText())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasPosition()) {
      hash = (37 * hash) + POSITION_FIELD_NUMBER;
      hash = (53 * hash) + getPosition().hashCode();
    }
    hash = (37 * hash) + FUNCTIONNAME_FIELD_NUMBER;
    hash = (53 * hash) + getFunctionName().hashCode();
    hash = (37 * hash) + EQUALITYOBJECT_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getEqualityObject());
    if (hasExtendedSourcePosition()) {
      hash = (37 * hash) + EXTENDEDSOURCEPOSITION_FIELD_NUMBER;
      hash = (53 * hash) + getExtendedSourcePosition().hashCode();
    }
    hash = (37 * hash) + SOURCEPOSITIONTEXT_FIELD_NUMBER;
    hash = (53 * hash) + getSourcePositionText().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.StackFrame parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(org.jetbrains.r.rinterop.StackFrame prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code rplugininterop.StackFrame}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rplugininterop.StackFrame)
      org.jetbrains.r.rinterop.StackFrameOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_StackFrame_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_StackFrame_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.jetbrains.r.rinterop.StackFrame.class, org.jetbrains.r.rinterop.StackFrame.Builder.class);
    }

    // Construct using org.jetbrains.r.rinterop.StackFrame.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (positionBuilder_ == null) {
        position_ = null;
      } else {
        position_ = null;
        positionBuilder_ = null;
      }
      functionName_ = "";

      equalityObject_ = 0L;

      if (extendedSourcePositionBuilder_ == null) {
        extendedSourcePosition_ = null;
      } else {
        extendedSourcePosition_ = null;
        extendedSourcePositionBuilder_ = null;
      }
      sourcePositionText_ = "";

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_StackFrame_descriptor;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.StackFrame getDefaultInstanceForType() {
      return org.jetbrains.r.rinterop.StackFrame.getDefaultInstance();
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.StackFrame build() {
      org.jetbrains.r.rinterop.StackFrame result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.StackFrame buildPartial() {
      org.jetbrains.r.rinterop.StackFrame result = new org.jetbrains.r.rinterop.StackFrame(this);
      if (positionBuilder_ == null) {
        result.position_ = position_;
      } else {
        result.position_ = positionBuilder_.build();
      }
      result.functionName_ = functionName_;
      result.equalityObject_ = equalityObject_;
      if (extendedSourcePositionBuilder_ == null) {
        result.extendedSourcePosition_ = extendedSourcePosition_;
      } else {
        result.extendedSourcePosition_ = extendedSourcePositionBuilder_.build();
      }
      result.sourcePositionText_ = sourcePositionText_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof org.jetbrains.r.rinterop.StackFrame) {
        return mergeFrom((org.jetbrains.r.rinterop.StackFrame)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.jetbrains.r.rinterop.StackFrame other) {
      if (other == org.jetbrains.r.rinterop.StackFrame.getDefaultInstance()) return this;
      if (other.hasPosition()) {
        mergePosition(other.getPosition());
      }
      if (!other.getFunctionName().isEmpty()) {
        functionName_ = other.functionName_;
        onChanged();
      }
      if (other.getEqualityObject() != 0L) {
        setEqualityObject(other.getEqualityObject());
      }
      if (other.hasExtendedSourcePosition()) {
        mergeExtendedSourcePosition(other.getExtendedSourcePosition());
      }
      if (!other.getSourcePositionText().isEmpty()) {
        sourcePositionText_ = other.sourcePositionText_;
        onChanged();
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.jetbrains.r.rinterop.StackFrame parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.r.rinterop.StackFrame) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private org.jetbrains.r.rinterop.SourcePosition position_;
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.SourcePosition, org.jetbrains.r.rinterop.SourcePosition.Builder, org.jetbrains.r.rinterop.SourcePositionOrBuilder> positionBuilder_;
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     * @return Whether the position field is set.
     */
    public boolean hasPosition() {
      return positionBuilder_ != null || position_ != null;
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     * @return The position.
     */
    public org.jetbrains.r.rinterop.SourcePosition getPosition() {
      if (positionBuilder_ == null) {
        return position_ == null ? org.jetbrains.r.rinterop.SourcePosition.getDefaultInstance() : position_;
      } else {
        return positionBuilder_.getMessage();
      }
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public Builder setPosition(org.jetbrains.r.rinterop.SourcePosition value) {
      if (positionBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        position_ = value;
        onChanged();
      } else {
        positionBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public Builder setPosition(
        org.jetbrains.r.rinterop.SourcePosition.Builder builderForValue) {
      if (positionBuilder_ == null) {
        position_ = builderForValue.build();
        onChanged();
      } else {
        positionBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public Builder mergePosition(org.jetbrains.r.rinterop.SourcePosition value) {
      if (positionBuilder_ == null) {
        if (position_ != null) {
          position_ =
            org.jetbrains.r.rinterop.SourcePosition.newBuilder(position_).mergeFrom(value).buildPartial();
        } else {
          position_ = value;
        }
        onChanged();
      } else {
        positionBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public Builder clearPosition() {
      if (positionBuilder_ == null) {
        position_ = null;
        onChanged();
      } else {
        position_ = null;
        positionBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public org.jetbrains.r.rinterop.SourcePosition.Builder getPositionBuilder() {
      
      onChanged();
      return getPositionFieldBuilder().getBuilder();
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    public org.jetbrains.r.rinterop.SourcePositionOrBuilder getPositionOrBuilder() {
      if (positionBuilder_ != null) {
        return positionBuilder_.getMessageOrBuilder();
      } else {
        return position_ == null ?
            org.jetbrains.r.rinterop.SourcePosition.getDefaultInstance() : position_;
      }
    }
    /**
     * <code>.rplugininterop.SourcePosition position = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.SourcePosition, org.jetbrains.r.rinterop.SourcePosition.Builder, org.jetbrains.r.rinterop.SourcePositionOrBuilder> 
        getPositionFieldBuilder() {
      if (positionBuilder_ == null) {
        positionBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            org.jetbrains.r.rinterop.SourcePosition, org.jetbrains.r.rinterop.SourcePosition.Builder, org.jetbrains.r.rinterop.SourcePositionOrBuilder>(
                getPosition(),
                getParentForChildren(),
                isClean());
        position_ = null;
      }
      return positionBuilder_;
    }

    private java.lang.Object functionName_ = "";
    /**
     * <code>string functionName = 2;</code>
     * @return The functionName.
     */
    public java.lang.String getFunctionName() {
      java.lang.Object ref = functionName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        functionName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string functionName = 2;</code>
     * @return The bytes for functionName.
     */
    public com.google.protobuf.ByteString
        getFunctionNameBytes() {
      java.lang.Object ref = functionName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        functionName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string functionName = 2;</code>
     * @param value The functionName to set.
     * @return This builder for chaining.
     */
    public Builder setFunctionName(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      functionName_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string functionName = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearFunctionName() {
      
      functionName_ = getDefaultInstance().getFunctionName();
      onChanged();
      return this;
    }
    /**
     * <code>string functionName = 2;</code>
     * @param value The bytes for functionName to set.
     * @return This builder for chaining.
     */
    public Builder setFunctionNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      functionName_ = value;
      onChanged();
      return this;
    }

    private long equalityObject_ ;
    /**
     * <code>int64 equalityObject = 3;</code>
     * @return The equalityObject.
     */
    @java.lang.Override
    public long getEqualityObject() {
      return equalityObject_;
    }
    /**
     * <code>int64 equalityObject = 3;</code>
     * @param value The equalityObject to set.
     * @return This builder for chaining.
     */
    public Builder setEqualityObject(long value) {
      
      equalityObject_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int64 equalityObject = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearEqualityObject() {
      
      equalityObject_ = 0L;
      onChanged();
      return this;
    }

    private org.jetbrains.r.rinterop.ExtendedSourcePosition extendedSourcePosition_;
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.ExtendedSourcePosition, org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder, org.jetbrains.r.rinterop.ExtendedSourcePositionOrBuilder> extendedSourcePositionBuilder_;
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     * @return Whether the extendedSourcePosition field is set.
     */
    public boolean hasExtendedSourcePosition() {
      return extendedSourcePositionBuilder_ != null || extendedSourcePosition_ != null;
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     * @return The extendedSourcePosition.
     */
    public org.jetbrains.r.rinterop.ExtendedSourcePosition getExtendedSourcePosition() {
      if (extendedSourcePositionBuilder_ == null) {
        return extendedSourcePosition_ == null ? org.jetbrains.r.rinterop.ExtendedSourcePosition.getDefaultInstance() : extendedSourcePosition_;
      } else {
        return extendedSourcePositionBuilder_.getMessage();
      }
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public Builder setExtendedSourcePosition(org.jetbrains.r.rinterop.ExtendedSourcePosition value) {
      if (extendedSourcePositionBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        extendedSourcePosition_ = value;
        onChanged();
      } else {
        extendedSourcePositionBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public Builder setExtendedSourcePosition(
        org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder builderForValue) {
      if (extendedSourcePositionBuilder_ == null) {
        extendedSourcePosition_ = builderForValue.build();
        onChanged();
      } else {
        extendedSourcePositionBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public Builder mergeExtendedSourcePosition(org.jetbrains.r.rinterop.ExtendedSourcePosition value) {
      if (extendedSourcePositionBuilder_ == null) {
        if (extendedSourcePosition_ != null) {
          extendedSourcePosition_ =
            org.jetbrains.r.rinterop.ExtendedSourcePosition.newBuilder(extendedSourcePosition_).mergeFrom(value).buildPartial();
        } else {
          extendedSourcePosition_ = value;
        }
        onChanged();
      } else {
        extendedSourcePositionBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public Builder clearExtendedSourcePosition() {
      if (extendedSourcePositionBuilder_ == null) {
        extendedSourcePosition_ = null;
        onChanged();
      } else {
        extendedSourcePosition_ = null;
        extendedSourcePositionBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder getExtendedSourcePositionBuilder() {
      
      onChanged();
      return getExtendedSourcePositionFieldBuilder().getBuilder();
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    public org.jetbrains.r.rinterop.ExtendedSourcePositionOrBuilder getExtendedSourcePositionOrBuilder() {
      if (extendedSourcePositionBuilder_ != null) {
        return extendedSourcePositionBuilder_.getMessageOrBuilder();
      } else {
        return extendedSourcePosition_ == null ?
            org.jetbrains.r.rinterop.ExtendedSourcePosition.getDefaultInstance() : extendedSourcePosition_;
      }
    }
    /**
     * <code>.rplugininterop.ExtendedSourcePosition extendedSourcePosition = 4;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.ExtendedSourcePosition, org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder, org.jetbrains.r.rinterop.ExtendedSourcePositionOrBuilder> 
        getExtendedSourcePositionFieldBuilder() {
      if (extendedSourcePositionBuilder_ == null) {
        extendedSourcePositionBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            org.jetbrains.r.rinterop.ExtendedSourcePosition, org.jetbrains.r.rinterop.ExtendedSourcePosition.Builder, org.jetbrains.r.rinterop.ExtendedSourcePositionOrBuilder>(
                getExtendedSourcePosition(),
                getParentForChildren(),
                isClean());
        extendedSourcePosition_ = null;
      }
      return extendedSourcePositionBuilder_;
    }

    private java.lang.Object sourcePositionText_ = "";
    /**
     * <code>string sourcePositionText = 5;</code>
     * @return The sourcePositionText.
     */
    public java.lang.String getSourcePositionText() {
      java.lang.Object ref = sourcePositionText_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        sourcePositionText_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string sourcePositionText = 5;</code>
     * @return The bytes for sourcePositionText.
     */
    public com.google.protobuf.ByteString
        getSourcePositionTextBytes() {
      java.lang.Object ref = sourcePositionText_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        sourcePositionText_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string sourcePositionText = 5;</code>
     * @param value The sourcePositionText to set.
     * @return This builder for chaining.
     */
    public Builder setSourcePositionText(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      sourcePositionText_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string sourcePositionText = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearSourcePositionText() {
      
      sourcePositionText_ = getDefaultInstance().getSourcePositionText();
      onChanged();
      return this;
    }
    /**
     * <code>string sourcePositionText = 5;</code>
     * @param value The bytes for sourcePositionText to set.
     * @return This builder for chaining.
     */
    public Builder setSourcePositionTextBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      sourcePositionText_ = value;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:rplugininterop.StackFrame)
  }

  // @@protoc_insertion_point(class_scope:rplugininterop.StackFrame)
  private static final org.jetbrains.r.rinterop.StackFrame DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.jetbrains.r.rinterop.StackFrame();
  }

  public static org.jetbrains.r.rinterop.StackFrame getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<StackFrame>
      PARSER = new com.google.protobuf.AbstractParser<StackFrame>() {
    @java.lang.Override
    public StackFrame parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new StackFrame(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<StackFrame> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<StackFrame> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.jetbrains.r.rinterop.StackFrame getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

