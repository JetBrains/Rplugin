// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

/**
 * Protobuf type {@code rplugininterop.GetSysEnvRequest}
 */
public final class GetSysEnvRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rplugininterop.GetSysEnvRequest)
    GetSysEnvRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use GetSysEnvRequest.newBuilder() to construct.
  private GetSysEnvRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private GetSysEnvRequest() {
    envName_ = "";
    flags_ = com.google.protobuf.LazyStringArrayList.EMPTY;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new GetSysEnvRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private GetSysEnvRequest(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
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
            java.lang.String s = input.readStringRequireUtf8();

            envName_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              flags_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000001;
            }
            flags_.add(s);
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
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        flags_ = flags_.getUnmodifiableView();
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_GetSysEnvRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_GetSysEnvRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.jetbrains.r.rinterop.GetSysEnvRequest.class, org.jetbrains.r.rinterop.GetSysEnvRequest.Builder.class);
  }

  public static final int ENVNAME_FIELD_NUMBER = 1;
  private volatile java.lang.Object envName_;
  /**
   * <code>string envName = 1;</code>
   * @return The envName.
   */
  @java.lang.Override
  public java.lang.String getEnvName() {
    java.lang.Object ref = envName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      envName_ = s;
      return s;
    }
  }
  /**
   * <code>string envName = 1;</code>
   * @return The bytes for envName.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getEnvNameBytes() {
    java.lang.Object ref = envName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      envName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int FLAGS_FIELD_NUMBER = 2;
  private com.google.protobuf.LazyStringList flags_;
  /**
   * <code>repeated string flags = 2;</code>
   * @return A list containing the flags.
   */
  public com.google.protobuf.ProtocolStringList
      getFlagsList() {
    return flags_;
  }
  /**
   * <code>repeated string flags = 2;</code>
   * @return The count of flags.
   */
  public int getFlagsCount() {
    return flags_.size();
  }
  /**
   * <code>repeated string flags = 2;</code>
   * @param index The index of the element to return.
   * @return The flags at the given index.
   */
  public java.lang.String getFlags(int index) {
    return flags_.get(index);
  }
  /**
   * <code>repeated string flags = 2;</code>
   * @param index The index of the value to return.
   * @return The bytes of the flags at the given index.
   */
  public com.google.protobuf.ByteString
      getFlagsBytes(int index) {
    return flags_.getByteString(index);
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
    if (!getEnvNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, envName_);
    }
    for (int i = 0; i < flags_.size(); i++) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, flags_.getRaw(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getEnvNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, envName_);
    }
    {
      int dataSize = 0;
      for (int i = 0; i < flags_.size(); i++) {
        dataSize += computeStringSizeNoTag(flags_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getFlagsList().size();
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
    if (!(obj instanceof org.jetbrains.r.rinterop.GetSysEnvRequest)) {
      return super.equals(obj);
    }
    org.jetbrains.r.rinterop.GetSysEnvRequest other = (org.jetbrains.r.rinterop.GetSysEnvRequest) obj;

    if (!getEnvName()
        .equals(other.getEnvName())) return false;
    if (!getFlagsList()
        .equals(other.getFlagsList())) return false;
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
    hash = (37 * hash) + ENVNAME_FIELD_NUMBER;
    hash = (53 * hash) + getEnvName().hashCode();
    if (getFlagsCount() > 0) {
      hash = (37 * hash) + FLAGS_FIELD_NUMBER;
      hash = (53 * hash) + getFlagsList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.GetSysEnvRequest parseFrom(
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
  public static Builder newBuilder(org.jetbrains.r.rinterop.GetSysEnvRequest prototype) {
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
   * Protobuf type {@code rplugininterop.GetSysEnvRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rplugininterop.GetSysEnvRequest)
      org.jetbrains.r.rinterop.GetSysEnvRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_GetSysEnvRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_GetSysEnvRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.jetbrains.r.rinterop.GetSysEnvRequest.class, org.jetbrains.r.rinterop.GetSysEnvRequest.Builder.class);
    }

    // Construct using org.jetbrains.r.rinterop.GetSysEnvRequest.newBuilder()
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
      envName_ = "";

      flags_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_GetSysEnvRequest_descriptor;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.GetSysEnvRequest getDefaultInstanceForType() {
      return org.jetbrains.r.rinterop.GetSysEnvRequest.getDefaultInstance();
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.GetSysEnvRequest build() {
      org.jetbrains.r.rinterop.GetSysEnvRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.GetSysEnvRequest buildPartial() {
      org.jetbrains.r.rinterop.GetSysEnvRequest result = new org.jetbrains.r.rinterop.GetSysEnvRequest(this);
      int from_bitField0_ = bitField0_;
      result.envName_ = envName_;
      if (((bitField0_ & 0x00000001) != 0)) {
        flags_ = flags_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.flags_ = flags_;
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
      if (other instanceof org.jetbrains.r.rinterop.GetSysEnvRequest) {
        return mergeFrom((org.jetbrains.r.rinterop.GetSysEnvRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.jetbrains.r.rinterop.GetSysEnvRequest other) {
      if (other == org.jetbrains.r.rinterop.GetSysEnvRequest.getDefaultInstance()) return this;
      if (!other.getEnvName().isEmpty()) {
        envName_ = other.envName_;
        onChanged();
      }
      if (!other.flags_.isEmpty()) {
        if (flags_.isEmpty()) {
          flags_ = other.flags_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureFlagsIsMutable();
          flags_.addAll(other.flags_);
        }
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
      org.jetbrains.r.rinterop.GetSysEnvRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.r.rinterop.GetSysEnvRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.lang.Object envName_ = "";
    /**
     * <code>string envName = 1;</code>
     * @return The envName.
     */
    public java.lang.String getEnvName() {
      java.lang.Object ref = envName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        envName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string envName = 1;</code>
     * @return The bytes for envName.
     */
    public com.google.protobuf.ByteString
        getEnvNameBytes() {
      java.lang.Object ref = envName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        envName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string envName = 1;</code>
     * @param value The envName to set.
     * @return This builder for chaining.
     */
    public Builder setEnvName(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      envName_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string envName = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearEnvName() {
      
      envName_ = getDefaultInstance().getEnvName();
      onChanged();
      return this;
    }
    /**
     * <code>string envName = 1;</code>
     * @param value The bytes for envName to set.
     * @return This builder for chaining.
     */
    public Builder setEnvNameBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      envName_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.LazyStringList flags_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    private void ensureFlagsIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        flags_ = new com.google.protobuf.LazyStringArrayList(flags_);
        bitField0_ |= 0x00000001;
       }
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @return A list containing the flags.
     */
    public com.google.protobuf.ProtocolStringList
        getFlagsList() {
      return flags_.getUnmodifiableView();
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @return The count of flags.
     */
    public int getFlagsCount() {
      return flags_.size();
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param index The index of the element to return.
     * @return The flags at the given index.
     */
    public java.lang.String getFlags(int index) {
      return flags_.get(index);
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param index The index of the value to return.
     * @return The bytes of the flags at the given index.
     */
    public com.google.protobuf.ByteString
        getFlagsBytes(int index) {
      return flags_.getByteString(index);
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param index The index to set the value at.
     * @param value The flags to set.
     * @return This builder for chaining.
     */
    public Builder setFlags(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureFlagsIsMutable();
      flags_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param value The flags to add.
     * @return This builder for chaining.
     */
    public Builder addFlags(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureFlagsIsMutable();
      flags_.add(value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param values The flags to add.
     * @return This builder for chaining.
     */
    public Builder addAllFlags(
        java.lang.Iterable<java.lang.String> values) {
      ensureFlagsIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(
          values, flags_);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearFlags() {
      flags_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string flags = 2;</code>
     * @param value The bytes of the flags to add.
     * @return This builder for chaining.
     */
    public Builder addFlagsBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      ensureFlagsIsMutable();
      flags_.add(value);
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


    // @@protoc_insertion_point(builder_scope:rplugininterop.GetSysEnvRequest)
  }

  // @@protoc_insertion_point(class_scope:rplugininterop.GetSysEnvRequest)
  private static final org.jetbrains.r.rinterop.GetSysEnvRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.jetbrains.r.rinterop.GetSysEnvRequest();
  }

  public static org.jetbrains.r.rinterop.GetSysEnvRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<GetSysEnvRequest>
      PARSER = new com.google.protobuf.AbstractParser<GetSysEnvRequest>() {
    @java.lang.Override
    public GetSysEnvRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new GetSysEnvRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<GetSysEnvRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<GetSysEnvRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.jetbrains.r.rinterop.GetSysEnvRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

