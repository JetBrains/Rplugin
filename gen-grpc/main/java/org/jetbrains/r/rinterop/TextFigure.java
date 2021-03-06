// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package org.jetbrains.r.rinterop;

/**
 * Protobuf type {@code rplugininterop.TextFigure}
 */
public final class TextFigure extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:rplugininterop.TextFigure)
    TextFigureOrBuilder {
private static final long serialVersionUID = 0L;
  // Use TextFigure.newBuilder() to construct.
  private TextFigure(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private TextFigure() {
    text_ = "";
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new TextFigure();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private TextFigure(
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
            java.lang.String s = input.readStringRequireUtf8();

            text_ = s;
            break;
          }
          case 17: {

            position_ = input.readFixed64();
            break;
          }
          case 29: {

            angle_ = input.readFloat();
            break;
          }
          case 37: {

            anchor_ = input.readFloat();
            break;
          }
          case 40: {

            fontIndex_ = input.readInt32();
            break;
          }
          case 48: {

            colorIndex_ = input.readInt32();
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
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_TextFigure_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_TextFigure_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.jetbrains.r.rinterop.TextFigure.class, org.jetbrains.r.rinterop.TextFigure.Builder.class);
  }

  public static final int TEXT_FIELD_NUMBER = 1;
  private volatile java.lang.Object text_;
  /**
   * <code>string text = 1;</code>
   * @return The text.
   */
  @java.lang.Override
  public java.lang.String getText() {
    java.lang.Object ref = text_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      text_ = s;
      return s;
    }
  }
  /**
   * <code>string text = 1;</code>
   * @return The bytes for text.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getTextBytes() {
    java.lang.Object ref = text_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      text_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int POSITION_FIELD_NUMBER = 2;
  private long position_;
  /**
   * <code>fixed64 position = 2;</code>
   * @return The position.
   */
  @java.lang.Override
  public long getPosition() {
    return position_;
  }

  public static final int ANGLE_FIELD_NUMBER = 3;
  private float angle_;
  /**
   * <code>float angle = 3;</code>
   * @return The angle.
   */
  @java.lang.Override
  public float getAngle() {
    return angle_;
  }

  public static final int ANCHOR_FIELD_NUMBER = 4;
  private float anchor_;
  /**
   * <code>float anchor = 4;</code>
   * @return The anchor.
   */
  @java.lang.Override
  public float getAnchor() {
    return anchor_;
  }

  public static final int FONTINDEX_FIELD_NUMBER = 5;
  private int fontIndex_;
  /**
   * <code>int32 fontIndex = 5;</code>
   * @return The fontIndex.
   */
  @java.lang.Override
  public int getFontIndex() {
    return fontIndex_;
  }

  public static final int COLORINDEX_FIELD_NUMBER = 6;
  private int colorIndex_;
  /**
   * <code>int32 colorIndex = 6;</code>
   * @return The colorIndex.
   */
  @java.lang.Override
  public int getColorIndex() {
    return colorIndex_;
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
    if (!getTextBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, text_);
    }
    if (position_ != 0L) {
      output.writeFixed64(2, position_);
    }
    if (angle_ != 0F) {
      output.writeFloat(3, angle_);
    }
    if (anchor_ != 0F) {
      output.writeFloat(4, anchor_);
    }
    if (fontIndex_ != 0) {
      output.writeInt32(5, fontIndex_);
    }
    if (colorIndex_ != 0) {
      output.writeInt32(6, colorIndex_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getTextBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, text_);
    }
    if (position_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeFixed64Size(2, position_);
    }
    if (angle_ != 0F) {
      size += com.google.protobuf.CodedOutputStream
        .computeFloatSize(3, angle_);
    }
    if (anchor_ != 0F) {
      size += com.google.protobuf.CodedOutputStream
        .computeFloatSize(4, anchor_);
    }
    if (fontIndex_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(5, fontIndex_);
    }
    if (colorIndex_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(6, colorIndex_);
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
    if (!(obj instanceof org.jetbrains.r.rinterop.TextFigure)) {
      return super.equals(obj);
    }
    org.jetbrains.r.rinterop.TextFigure other = (org.jetbrains.r.rinterop.TextFigure) obj;

    if (!getText()
        .equals(other.getText())) return false;
    if (getPosition()
        != other.getPosition()) return false;
    if (java.lang.Float.floatToIntBits(getAngle())
        != java.lang.Float.floatToIntBits(
            other.getAngle())) return false;
    if (java.lang.Float.floatToIntBits(getAnchor())
        != java.lang.Float.floatToIntBits(
            other.getAnchor())) return false;
    if (getFontIndex()
        != other.getFontIndex()) return false;
    if (getColorIndex()
        != other.getColorIndex()) return false;
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
    hash = (37 * hash) + TEXT_FIELD_NUMBER;
    hash = (53 * hash) + getText().hashCode();
    hash = (37 * hash) + POSITION_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getPosition());
    hash = (37 * hash) + ANGLE_FIELD_NUMBER;
    hash = (53 * hash) + java.lang.Float.floatToIntBits(
        getAngle());
    hash = (37 * hash) + ANCHOR_FIELD_NUMBER;
    hash = (53 * hash) + java.lang.Float.floatToIntBits(
        getAnchor());
    hash = (37 * hash) + FONTINDEX_FIELD_NUMBER;
    hash = (53 * hash) + getFontIndex();
    hash = (37 * hash) + COLORINDEX_FIELD_NUMBER;
    hash = (53 * hash) + getColorIndex();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.jetbrains.r.rinterop.TextFigure parseFrom(
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
  public static Builder newBuilder(org.jetbrains.r.rinterop.TextFigure prototype) {
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
   * Protobuf type {@code rplugininterop.TextFigure}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:rplugininterop.TextFigure)
      org.jetbrains.r.rinterop.TextFigureOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_TextFigure_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_TextFigure_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.jetbrains.r.rinterop.TextFigure.class, org.jetbrains.r.rinterop.TextFigure.Builder.class);
    }

    // Construct using org.jetbrains.r.rinterop.TextFigure.newBuilder()
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
      text_ = "";

      position_ = 0L;

      angle_ = 0F;

      anchor_ = 0F;

      fontIndex_ = 0;

      colorIndex_ = 0;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.jetbrains.r.rinterop.Service.internal_static_rplugininterop_TextFigure_descriptor;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.TextFigure getDefaultInstanceForType() {
      return org.jetbrains.r.rinterop.TextFigure.getDefaultInstance();
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.TextFigure build() {
      org.jetbrains.r.rinterop.TextFigure result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public org.jetbrains.r.rinterop.TextFigure buildPartial() {
      org.jetbrains.r.rinterop.TextFigure result = new org.jetbrains.r.rinterop.TextFigure(this);
      result.text_ = text_;
      result.position_ = position_;
      result.angle_ = angle_;
      result.anchor_ = anchor_;
      result.fontIndex_ = fontIndex_;
      result.colorIndex_ = colorIndex_;
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
      if (other instanceof org.jetbrains.r.rinterop.TextFigure) {
        return mergeFrom((org.jetbrains.r.rinterop.TextFigure)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.jetbrains.r.rinterop.TextFigure other) {
      if (other == org.jetbrains.r.rinterop.TextFigure.getDefaultInstance()) return this;
      if (!other.getText().isEmpty()) {
        text_ = other.text_;
        onChanged();
      }
      if (other.getPosition() != 0L) {
        setPosition(other.getPosition());
      }
      if (other.getAngle() != 0F) {
        setAngle(other.getAngle());
      }
      if (other.getAnchor() != 0F) {
        setAnchor(other.getAnchor());
      }
      if (other.getFontIndex() != 0) {
        setFontIndex(other.getFontIndex());
      }
      if (other.getColorIndex() != 0) {
        setColorIndex(other.getColorIndex());
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
      org.jetbrains.r.rinterop.TextFigure parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.r.rinterop.TextFigure) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object text_ = "";
    /**
     * <code>string text = 1;</code>
     * @return The text.
     */
    public java.lang.String getText() {
      java.lang.Object ref = text_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        text_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string text = 1;</code>
     * @return The bytes for text.
     */
    public com.google.protobuf.ByteString
        getTextBytes() {
      java.lang.Object ref = text_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        text_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string text = 1;</code>
     * @param value The text to set.
     * @return This builder for chaining.
     */
    public Builder setText(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      text_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string text = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearText() {
      
      text_ = getDefaultInstance().getText();
      onChanged();
      return this;
    }
    /**
     * <code>string text = 1;</code>
     * @param value The bytes for text to set.
     * @return This builder for chaining.
     */
    public Builder setTextBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      text_ = value;
      onChanged();
      return this;
    }

    private long position_ ;
    /**
     * <code>fixed64 position = 2;</code>
     * @return The position.
     */
    @java.lang.Override
    public long getPosition() {
      return position_;
    }
    /**
     * <code>fixed64 position = 2;</code>
     * @param value The position to set.
     * @return This builder for chaining.
     */
    public Builder setPosition(long value) {
      
      position_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>fixed64 position = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearPosition() {
      
      position_ = 0L;
      onChanged();
      return this;
    }

    private float angle_ ;
    /**
     * <code>float angle = 3;</code>
     * @return The angle.
     */
    @java.lang.Override
    public float getAngle() {
      return angle_;
    }
    /**
     * <code>float angle = 3;</code>
     * @param value The angle to set.
     * @return This builder for chaining.
     */
    public Builder setAngle(float value) {
      
      angle_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>float angle = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearAngle() {
      
      angle_ = 0F;
      onChanged();
      return this;
    }

    private float anchor_ ;
    /**
     * <code>float anchor = 4;</code>
     * @return The anchor.
     */
    @java.lang.Override
    public float getAnchor() {
      return anchor_;
    }
    /**
     * <code>float anchor = 4;</code>
     * @param value The anchor to set.
     * @return This builder for chaining.
     */
    public Builder setAnchor(float value) {
      
      anchor_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>float anchor = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearAnchor() {
      
      anchor_ = 0F;
      onChanged();
      return this;
    }

    private int fontIndex_ ;
    /**
     * <code>int32 fontIndex = 5;</code>
     * @return The fontIndex.
     */
    @java.lang.Override
    public int getFontIndex() {
      return fontIndex_;
    }
    /**
     * <code>int32 fontIndex = 5;</code>
     * @param value The fontIndex to set.
     * @return This builder for chaining.
     */
    public Builder setFontIndex(int value) {
      
      fontIndex_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 fontIndex = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearFontIndex() {
      
      fontIndex_ = 0;
      onChanged();
      return this;
    }

    private int colorIndex_ ;
    /**
     * <code>int32 colorIndex = 6;</code>
     * @return The colorIndex.
     */
    @java.lang.Override
    public int getColorIndex() {
      return colorIndex_;
    }
    /**
     * <code>int32 colorIndex = 6;</code>
     * @param value The colorIndex to set.
     * @return This builder for chaining.
     */
    public Builder setColorIndex(int value) {
      
      colorIndex_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int32 colorIndex = 6;</code>
     * @return This builder for chaining.
     */
    public Builder clearColorIndex() {
      
      colorIndex_ = 0;
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


    // @@protoc_insertion_point(builder_scope:rplugininterop.TextFigure)
  }

  // @@protoc_insertion_point(class_scope:rplugininterop.TextFigure)
  private static final org.jetbrains.r.rinterop.TextFigure DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.jetbrains.r.rinterop.TextFigure();
  }

  public static org.jetbrains.r.rinterop.TextFigure getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<TextFigure>
      PARSER = new com.google.protobuf.AbstractParser<TextFigure>() {
    @java.lang.Override
    public TextFigure parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new TextFigure(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<TextFigure> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<TextFigure> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public org.jetbrains.r.rinterop.TextFigure getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

