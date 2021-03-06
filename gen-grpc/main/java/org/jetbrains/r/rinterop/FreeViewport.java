// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

/**
 * Protobuf type {@code rplugininterop.FreeViewport}
 */
public final class FreeViewport extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rplugininterop.FreeViewport)
    FreeViewportOrBuilder {
private static final long serialVersionUID = 0L;
  // Use FreeViewport.newBuilder() to construct.
  private FreeViewport(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private FreeViewport() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new FreeViewport();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private FreeViewport(
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
          case 9: {

            from_ = input.readFixed64();
            break;
          }
          case 17: {

            to_ = input.readFixed64();
            break;
          }
          case 24: {

            parentIndex_ = input.readInt32();
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
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_FreeViewport_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_FreeViewport_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.jetbrains.r.rinterop.FreeViewport.class, org.jetbrains.r.rinterop.FreeViewport.Builder.class);
  }

  public static final int FROM_FIELD_NUMBER = 1;
  private long from_;
  /**
   * <code>fixed64 from = 1;</code>
   * @return The from.
   */
  @java.lang.Override
  public long getFrom() {
    return from_;
  }

  public static final int TO_FIELD_NUMBER = 2;
  private long to_;
  /**
   * <code>fixed64 to = 2;</code>
   * @return The to.
   */
  @java.lang.Override
  public long getTo() {
    return to_;
  }

  public static final int PARENTINDEX_FIELD_NUMBER = 3;
  private int parentIndex_;
  /**
   * <code>int32 parentIndex = 3;</code>
   * @return The parentIndex.
   */
  @java.lang.Override
  public int getParentIndex() {
    return parentIndex_;
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
    if (from_ != 0L) {
      output.writeFixed64(1, from_);
    }
    if (to_ != 0L) {
      output.writeFixed64(2, to_);
    }
    if (parentIndex_ != 0) {
      output.writeInt32(3, parentIndex_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (from_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeFixed64Size(1, from_);
    }
    if (to_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeFixed64Size(2, to_);
    }
    if (parentIndex_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(3, parentIndex_);
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
    if (!(obj instanceof org.jetbrains.r.rinterop.FreeViewport)) {
      return super.equals(obj);
    }
    org.jetbrains.r.rinterop.FreeViewport other = (org.jetbrains.r.rinterop.FreeViewport) obj;

    if (getFrom()
        != other.getFrom()) return false;
    if (getTo()
        != other.getTo()) return false;
    if (getParentIndex()
        != other.getParentIndex()) return false;
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
    hash = (37 * hash) + FROM_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getFrom());
    hash = (37 * hash) + TO_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getTo());
    hash = (37 * hash) + PARENTINDEX_FIELD_NUMBER;
    hash = (53 * hash) + getParentIndex();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.FreeViewport parseFrom(
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
  public static Builder newBuilder(org.jetbrains.r.rinterop.FreeViewport prototype) {
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
   * Protobuf type {@code rplugininterop.FreeViewport}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rplugininterop.FreeViewport)
      org.jetbrains.r.rinterop.FreeViewportOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_FreeViewport_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_FreeViewport_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.jetbrains.r.rinterop.FreeViewport.class, org.jetbrains.r.rinterop.FreeViewport.Builder.class);
    }

    // Construct using org.jetbrains.r.rinterop.FreeViewport.newBuilder()
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
      from_ = 0L;

      to_ = 0L;

      parentIndex_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_FreeViewport_descriptor;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.FreeViewport getDefaultInstanceForType() {
      return org.jetbrains.r.rinterop.FreeViewport.getDefaultInstance();
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.FreeViewport build() {
      org.jetbrains.r.rinterop.FreeViewport result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.FreeViewport buildPartial() {
      org.jetbrains.r.rinterop.FreeViewport result = new org.jetbrains.r.rinterop.FreeViewport(this);
      result.from_ = from_;
      result.to_ = to_;
      result.parentIndex_ = parentIndex_;
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
      if (other instanceof org.jetbrains.r.rinterop.FreeViewport) {
        return mergeFrom((org.jetbrains.r.rinterop.FreeViewport)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.jetbrains.r.rinterop.FreeViewport other) {
      if (other == org.jetbrains.r.rinterop.FreeViewport.getDefaultInstance()) return this;
      if (other.getFrom() != 0L) {
        setFrom(other.getFrom());
      }
      if (other.getTo() != 0L) {
        setTo(other.getTo());
      }
      if (other.getParentIndex() != 0) {
        setParentIndex(other.getParentIndex());
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
      org.jetbrains.r.rinterop.FreeViewport parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.r.rinterop.FreeViewport) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private long from_ ;
    /**
     * <code>fixed64 from = 1;</code>
     * @return The from.
     */
    @java.lang.Override
    public long getFrom() {
      return from_;
    }
    /**
     * <code>fixed64 from = 1;</code>
     * @param value The from to set.
     * @return This builder for chaining.
     */
    public Builder setFrom(long value) {
      
      from_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>fixed64 from = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearFrom() {
      
      from_ = 0L;
      onChanged();
      return this;
    }

    private long to_ ;
    /**
     * <code>fixed64 to = 2;</code>
     * @return The to.
     */
    @java.lang.Override
    public long getTo() {
      return to_;
    }
    /**
     * <code>fixed64 to = 2;</code>
     * @param value The to to set.
     * @return This builder for chaining.
     */
    public Builder setTo(long value) {
      
      to_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>fixed64 to = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearTo() {
      
      to_ = 0L;
      onChanged();
      return this;
    }

    private int parentIndex_ ;
    /**
     * <code>int32 parentIndex = 3;</code>
     * @return The parentIndex.
     */
    @java.lang.Override
    public int getParentIndex() {
      return parentIndex_;
    }
    /**
     * <code>int32 parentIndex = 3;</code>
     * @param value The parentIndex to set.
     * @return This builder for chaining.
     */
    public Builder setParentIndex(int value) {
      
      parentIndex_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 parentIndex = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearParentIndex() {
      
      parentIndex_ = 0;
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


    // @@protoc_insertion_point(builder_scope:rplugininterop.FreeViewport)
  }

  // @@protoc_insertion_point(class_scope:rplugininterop.FreeViewport)
  private static final org.jetbrains.r.rinterop.FreeViewport DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.jetbrains.r.rinterop.FreeViewport();
  }

  public static org.jetbrains.r.rinterop.FreeViewport getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<FreeViewport>
      PARSER = new com.google.protobuf.AbstractParser<FreeViewport>() {
    @java.lang.Override
    public FreeViewport parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new FreeViewport(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<FreeViewport> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<FreeViewport> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.jetbrains.r.rinterop.FreeViewport getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

