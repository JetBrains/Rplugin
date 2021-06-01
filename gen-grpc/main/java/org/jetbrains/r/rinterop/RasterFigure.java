// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

/**
 * Protobuf type {@code rplugininterop.RasterFigure}
 */
public final class RasterFigure extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rplugininterop.RasterFigure)
    RasterFigureOrBuilder {
private static final long serialVersionUID = 0L;
  // Use RasterFigure.newBuilder() to construct.
  private RasterFigure(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private RasterFigure() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new RasterFigure();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private RasterFigure(
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
            org.jetbrains.r.rinterop.RasterImage.Builder subBuilder = null;
            if (image_ != null) {
              subBuilder = image_.toBuilder();
            }
            image_ = input.readMessage(org.jetbrains.r.rinterop.RasterImage.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(image_);
              image_ = subBuilder.buildPartial();
            }

            break;
          }
          case 17: {

            from_ = input.readFixed64();
            break;
          }
          case 25: {

            to_ = input.readFixed64();
            break;
          }
          case 37: {

            angle_ = input.readFloat();
            break;
          }
          case 40: {

            interpolate_ = input.readBool();
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
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_RasterFigure_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_RasterFigure_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.jetbrains.r.rinterop.RasterFigure.class, org.jetbrains.r.rinterop.RasterFigure.Builder.class);
  }

  public static final int IMAGE_FIELD_NUMBER = 1;
  private org.jetbrains.r.rinterop.RasterImage image_;
  /**
   * <code>.rplugininterop.RasterImage image = 1;</code>
   * @return Whether the image field is set.
   */
  @java.lang.Override
  public boolean hasImage() {
    return image_ != null;
  }
  /**
   * <code>.rplugininterop.RasterImage image = 1;</code>
   * @return The image.
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.RasterImage getImage() {
    return image_ == null ? org.jetbrains.r.rinterop.RasterImage.getDefaultInstance() : image_;
  }
  /**
   * <code>.rplugininterop.RasterImage image = 1;</code>
   */
  @java.lang.Override
  public org.jetbrains.r.rinterop.RasterImageOrBuilder getImageOrBuilder() {
    return getImage();
  }

  public static final int FROM_FIELD_NUMBER = 2;
  private long from_;
  /**
   * <code>fixed64 from = 2;</code>
   * @return The from.
   */
  @java.lang.Override
  public long getFrom() {
    return from_;
  }

  public static final int TO_FIELD_NUMBER = 3;
  private long to_;
  /**
   * <code>fixed64 to = 3;</code>
   * @return The to.
   */
  @java.lang.Override
  public long getTo() {
    return to_;
  }

  public static final int ANGLE_FIELD_NUMBER = 4;
  private float angle_;
  /**
   * <code>float angle = 4;</code>
   * @return The angle.
   */
  @java.lang.Override
  public float getAngle() {
    return angle_;
  }

  public static final int INTERPOLATE_FIELD_NUMBER = 5;
  private boolean interpolate_;
  /**
   * <code>bool interpolate = 5;</code>
   * @return The interpolate.
   */
  @java.lang.Override
  public boolean getInterpolate() {
    return interpolate_;
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
    if (image_ != null) {
      output.writeMessage(1, getImage());
    }
    if (from_ != 0L) {
      output.writeFixed64(2, from_);
    }
    if (to_ != 0L) {
      output.writeFixed64(3, to_);
    }
    if (angle_ != 0F) {
      output.writeFloat(4, angle_);
    }
    if (interpolate_ != false) {
      output.writeBool(5, interpolate_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (image_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getImage());
    }
    if (from_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeFixed64Size(2, from_);
    }
    if (to_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeFixed64Size(3, to_);
    }
    if (angle_ != 0F) {
      size += com.google.protobuf.CodedOutputStream
        .computeFloatSize(4, angle_);
    }
    if (interpolate_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(5, interpolate_);
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
    if (!(obj instanceof org.jetbrains.r.rinterop.RasterFigure)) {
      return super.equals(obj);
    }
    org.jetbrains.r.rinterop.RasterFigure other = (org.jetbrains.r.rinterop.RasterFigure) obj;

    if (hasImage() != other.hasImage()) return false;
    if (hasImage()) {
      if (!getImage()
          .equals(other.getImage())) return false;
    }
    if (getFrom()
        != other.getFrom()) return false;
    if (getTo()
        != other.getTo()) return false;
    if (java.lang.Float.floatToIntBits(getAngle())
        != java.lang.Float.floatToIntBits(
            other.getAngle())) return false;
    if (getInterpolate()
        != other.getInterpolate()) return false;
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
    if (hasImage()) {
      hash = (37 * hash) + IMAGE_FIELD_NUMBER;
      hash = (53 * hash) + getImage().hashCode();
    }
    hash = (37 * hash) + FROM_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getFrom());
    hash = (37 * hash) + TO_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getTo());
    hash = (37 * hash) + ANGLE_FIELD_NUMBER;
    hash = (53 * hash) + java.lang.Float.floatToIntBits(
        getAngle());
    hash = (37 * hash) + INTERPOLATE_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getInterpolate());
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.RasterFigure parseFrom(
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
  public static Builder newBuilder(org.jetbrains.r.rinterop.RasterFigure prototype) {
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
   * Protobuf type {@code rplugininterop.RasterFigure}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rplugininterop.RasterFigure)
      org.jetbrains.r.rinterop.RasterFigureOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_RasterFigure_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_RasterFigure_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.jetbrains.r.rinterop.RasterFigure.class, org.jetbrains.r.rinterop.RasterFigure.Builder.class);
    }

    // Construct using org.jetbrains.r.rinterop.RasterFigure.newBuilder()
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
      if (imageBuilder_ == null) {
        image_ = null;
      } else {
        image_ = null;
        imageBuilder_ = null;
      }
      from_ = 0L;

      to_ = 0L;

      angle_ = 0F;

      interpolate_ = false;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_RasterFigure_descriptor;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.RasterFigure getDefaultInstanceForType() {
      return org.jetbrains.r.rinterop.RasterFigure.getDefaultInstance();
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.RasterFigure build() {
      org.jetbrains.r.rinterop.RasterFigure result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.RasterFigure buildPartial() {
      org.jetbrains.r.rinterop.RasterFigure result = new org.jetbrains.r.rinterop.RasterFigure(this);
      if (imageBuilder_ == null) {
        result.image_ = image_;
      } else {
        result.image_ = imageBuilder_.build();
      }
      result.from_ = from_;
      result.to_ = to_;
      result.angle_ = angle_;
      result.interpolate_ = interpolate_;
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
      if (other instanceof org.jetbrains.r.rinterop.RasterFigure) {
        return mergeFrom((org.jetbrains.r.rinterop.RasterFigure)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.jetbrains.r.rinterop.RasterFigure other) {
      if (other == org.jetbrains.r.rinterop.RasterFigure.getDefaultInstance()) return this;
      if (other.hasImage()) {
        mergeImage(other.getImage());
      }
      if (other.getFrom() != 0L) {
        setFrom(other.getFrom());
      }
      if (other.getTo() != 0L) {
        setTo(other.getTo());
      }
      if (other.getAngle() != 0F) {
        setAngle(other.getAngle());
      }
      if (other.getInterpolate() != false) {
        setInterpolate(other.getInterpolate());
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
      org.jetbrains.r.rinterop.RasterFigure parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.r.rinterop.RasterFigure) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private org.jetbrains.r.rinterop.RasterImage image_;
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.RasterImage, org.jetbrains.r.rinterop.RasterImage.Builder, org.jetbrains.r.rinterop.RasterImageOrBuilder> imageBuilder_;
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     * @return Whether the image field is set.
     */
    public boolean hasImage() {
      return imageBuilder_ != null || image_ != null;
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     * @return The image.
     */
    public org.jetbrains.r.rinterop.RasterImage getImage() {
      if (imageBuilder_ == null) {
        return image_ == null ? org.jetbrains.r.rinterop.RasterImage.getDefaultInstance() : image_;
      } else {
        return imageBuilder_.getMessage();
      }
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public Builder setImage(org.jetbrains.r.rinterop.RasterImage value) {
      if (imageBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        image_ = value;
        onChanged();
      } else {
        imageBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public Builder setImage(
        org.jetbrains.r.rinterop.RasterImage.Builder builderForValue) {
      if (imageBuilder_ == null) {
        image_ = builderForValue.build();
        onChanged();
      } else {
        imageBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public Builder mergeImage(org.jetbrains.r.rinterop.RasterImage value) {
      if (imageBuilder_ == null) {
        if (image_ != null) {
          image_ =
            org.jetbrains.r.rinterop.RasterImage.newBuilder(image_).mergeFrom(value).buildPartial();
        } else {
          image_ = value;
        }
        onChanged();
      } else {
        imageBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public Builder clearImage() {
      if (imageBuilder_ == null) {
        image_ = null;
        onChanged();
      } else {
        image_ = null;
        imageBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public org.jetbrains.r.rinterop.RasterImage.Builder getImageBuilder() {
      
      onChanged();
      return getImageFieldBuilder().getBuilder();
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    public org.jetbrains.r.rinterop.RasterImageOrBuilder getImageOrBuilder() {
      if (imageBuilder_ != null) {
        return imageBuilder_.getMessageOrBuilder();
      } else {
        return image_ == null ?
            org.jetbrains.r.rinterop.RasterImage.getDefaultInstance() : image_;
      }
    }
    /**
     * <code>.rplugininterop.RasterImage image = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        org.jetbrains.r.rinterop.RasterImage, org.jetbrains.r.rinterop.RasterImage.Builder, org.jetbrains.r.rinterop.RasterImageOrBuilder> 
        getImageFieldBuilder() {
      if (imageBuilder_ == null) {
        imageBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            org.jetbrains.r.rinterop.RasterImage, org.jetbrains.r.rinterop.RasterImage.Builder, org.jetbrains.r.rinterop.RasterImageOrBuilder>(
                getImage(),
                getParentForChildren(),
                isClean());
        image_ = null;
      }
      return imageBuilder_;
    }

    private long from_ ;
    /**
     * <code>fixed64 from = 2;</code>
     * @return The from.
     */
    @java.lang.Override
    public long getFrom() {
      return from_;
    }
    /**
     * <code>fixed64 from = 2;</code>
     * @param value The from to set.
     * @return This builder for chaining.
     */
    public Builder setFrom(long value) {
      
      from_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>fixed64 from = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearFrom() {
      
      from_ = 0L;
      onChanged();
      return this;
    }

    private long to_ ;
    /**
     * <code>fixed64 to = 3;</code>
     * @return The to.
     */
    @java.lang.Override
    public long getTo() {
      return to_;
    }
    /**
     * <code>fixed64 to = 3;</code>
     * @param value The to to set.
     * @return This builder for chaining.
     */
    public Builder setTo(long value) {
      
      to_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>fixed64 to = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearTo() {
      
      to_ = 0L;
      onChanged();
      return this;
    }

    private float angle_ ;
    /**
     * <code>float angle = 4;</code>
     * @return The angle.
     */
    @java.lang.Override
    public float getAngle() {
      return angle_;
    }
    /**
     * <code>float angle = 4;</code>
     * @param value The angle to set.
     * @return This builder for chaining.
     */
    public Builder setAngle(float value) {
      
      angle_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>float angle = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearAngle() {
      
      angle_ = 0F;
      onChanged();
      return this;
    }

    private boolean interpolate_ ;
    /**
     * <code>bool interpolate = 5;</code>
     * @return The interpolate.
     */
    @java.lang.Override
    public boolean getInterpolate() {
      return interpolate_;
    }
    /**
     * <code>bool interpolate = 5;</code>
     * @param value The interpolate to set.
     * @return This builder for chaining.
     */
    public Builder setInterpolate(boolean value) {
      
      interpolate_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bool interpolate = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearInterpolate() {
      
      interpolate_ = false;
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


    // @@protoc_insertion_point(builder_scope:rplugininterop.RasterFigure)
  }

  // @@protoc_insertion_point(class_scope:rplugininterop.RasterFigure)
  private static final org.jetbrains.r.rinterop.RasterFigure DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.jetbrains.r.rinterop.RasterFigure();
  }

  public static org.jetbrains.r.rinterop.RasterFigure getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RasterFigure>
      PARSER = new com.google.protobuf.AbstractParser<RasterFigure>() {
    @java.lang.Override
    public RasterFigure parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new RasterFigure(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<RasterFigure> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<RasterFigure> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.jetbrains.r.rinterop.RasterFigure getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
