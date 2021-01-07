package nbts

import org.apache.commons.lang3.ArrayUtils
import scala.collection.mutable

sealed trait Tag

object EndTag extends Tag

final case class StringTag private (data: String) extends Tag

object StringTag:
  val Empty = new StringTag("")
  def apply(data: String): StringTag = if data.isEmpty then Empty else new StringTag(data)

final case class CompoundTag private (data: mutable.Map[String, Tag]) extends Tag:
  def put(key: String, value: Tag): Option[Tag] = data.put(key, value)

  def get(key: String): Option[Tag] = data.get(key)

  def -=(key: String): Unit = data -= key

  def contains(key: String): Boolean = data contains key

object CompoundTag:
  def apply(): CompoundTag = new CompoundTag(mutable.Map.empty)

sealed trait NumericTag extends Tag:
  def asByte: Byte
  def asShort: Short
  def asInt: Int
  def asLong: Long
  def asFloat: Float
  def asDouble: Double
  def asNumber: Number

final case class ByteTag private (data: Byte) extends NumericTag:
  def asByte: Byte = data
  def asShort: Short = data
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object ByteTag:
  inline val MinCache = -128
  inline val MaxCache = 127
  private val Cache: Seq[ByteTag] = (MinCache to MaxCache).map(data => new ByteTag(data.toByte))
  val Zero: ByteTag = ByteTag(0.toByte)
  val One: ByteTag = ByteTag(1.toByte)
  def apply(data: Byte): ByteTag = Cache(data + 128)
  def apply(data: Boolean): ByteTag = if data then One else Zero

final case class ShortTag private (data: Short) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = data
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object ShortTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[ShortTag] = (MinCache to MaxCache).map(data => new ShortTag(data.toShort))
  def apply(data: Short): ShortTag = Cache.lift(data - MinCache).getOrElse(new ShortTag(data))

final case class IntTag private (data: Int) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = (data & 0xffff).toShort
  def asInt: Int = data
  def asLong: Long = data
  def asFloat: Float = data.toFloat
  def asDouble: Double = data
  def asNumber: Number = data

object IntTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[IntTag] = (MinCache to MaxCache).map(data => new IntTag(data))
  def apply(data: Int): IntTag = Cache.lift(data - MinCache).getOrElse(new IntTag(data))

final case class LongTag private (data: Long) extends NumericTag:
  def asByte: Byte = (data & 0xff).toByte
  def asShort: Short = (data & 0xffff).toShort
  def asInt: Int = data.toInt
  def asLong: Long = data
  def asFloat: Float = data.toFloat
  def asDouble: Double = data.toDouble
  def asNumber: Number = data

object LongTag:
  inline val MinCache = -128
  inline val MaxCache = 1024
  private val Cache: Seq[LongTag] = (MinCache to MaxCache).map(data => new LongTag(data))
  def apply(data: Long): LongTag = Cache.lift(data.toInt - MinCache).getOrElse(new LongTag(data))

final case class FloatTag private (data: Float) extends NumericTag:
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = data.toLong
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object FloatTag:
  val Zero: FloatTag = new FloatTag(0.0f)
  def apply(data: Float): FloatTag = if data == 0.0f then Zero else new FloatTag(data)

final case class DoubleTag private (data: Double) extends NumericTag:
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = Math.floor(data).toLong
  def asFloat: Float = data.toFloat
  def asDouble: Double = data
  def asNumber: Number = data

object DoubleTag:
  val Zero: DoubleTag = new DoubleTag(0.0)
  def apply(data: Double): DoubleTag = if data == 0.0 then Zero else new DoubleTag(data)

sealed trait CollectionTag[T <: Tag] extends Tag:
  def set(index: Int, element: T): T
  def add(index: Int, element: T): Unit
  def remove(index: Int): T
  def setTag(index: Int, tag: Tag): Boolean
  def addTag(index: Int, tag: Tag): Boolean

final case class ByteArrayTag(private var data: Array[Byte]) extends CollectionTag[ByteTag]:
  def size: Int = data.size

  def get(index: Int): ByteTag = ByteTag(data(index))

  def set(index: Int, element: ByteTag): ByteTag =
    val old = data(index)
    data(index) = element.asByte
    ByteTag(old)

  def add(index: Int, element: ByteTag): Unit =
    data = ArrayUtils.insert(index, data, element.asByte)

  def remove(index: Int): ByteTag =
    val old = data(index)
    data = ArrayUtils.remove(data, index)
    ByteTag(old)

  def setTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => data(index) = tag.asByte; true
    case _ => false

  def addTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => ArrayUtils.insert(index, data, tag.asByte); true
    case _ => false

final case class IntArrayTag(private var data: Array[Int]) extends CollectionTag[IntTag]:
  def size: Int = data.size

  def get(index: Int): IntTag = IntTag(data(index))

  def set(index: Int, element: IntTag): IntTag =
    val old = data(index)
    data(index) = element.asInt
    IntTag(old)

  def add(index: Int, element: IntTag): Unit =
    data = ArrayUtils.insert(index, data, element.asInt)

  def remove(index: Int): IntTag =
    val old = data(index)
    data = ArrayUtils.remove(data, index)
    IntTag(old)

  def setTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => data(index) = tag.asInt; true
    case _ => false

  def addTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => ArrayUtils.insert(index, data, tag.asInt); true
    case _ => false

final case class LongArrayTag(private var data: Array[Long]) extends CollectionTag[LongTag]:
  def size: Int = data.size

  def get(index: Int): LongTag = LongTag(data(index))

  def set(index: Int, element: LongTag): LongTag =
    val old = data(index)
    data(index) = element.asLong
    LongTag(old)

  def add(index: Int, element: LongTag): Unit =
    data = ArrayUtils.insert(index, data, element.asLong)

  def remove(index: Int): LongTag =
    val old = data(index)
    data = ArrayUtils.remove(data, index)
    LongTag(old)

  def setTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => data(index) = tag.asLong; true
    case _ => false

  def addTag(index: Int, tag: Tag): Boolean =
    tag match
    case tag: NumericTag => ArrayUtils.insert(index, data, tag.asLong); true
    case _ => false

final case class ListTag private (data: mutable.AbstractBuffer[Tag], var elementClass: Class[? <: Tag]) extends CollectionTag[Tag]:
  def apply(): ListTag = ListTag(mutable.ArrayBuffer.empty, EndTag.getClass)

  def size: Int = data.size

  def get(index: Int): Tag = data(index)

  def set(index: Int, element: Tag): Tag =
    val old = data(index)
    if setTag(index, element) then old
    else throw UnsupportedOperationException()

  def add(index: Int, element: Tag): Unit =
    if !addTag(index, element) then throw UnsupportedOperationException()

  def remove(index: Int): Tag =
    val old = data.remove(index)
    if data.isEmpty then elementClass = EndTag.getClass
    old

  def setTag(index: Int, tag: Tag): Boolean =
    if !updateClass(tag) then false
    else data(index) = tag; true

  def addTag(index: Int, tag: Tag): Boolean =
    if !updateClass(tag) then false
    else data.insert(index, tag); true

  private def updateClass(tag: Tag): Boolean =
    if tag.getClass == EndTag.getClass then false
    else if elementClass == EndTag.getClass then
      elementClass = tag.getClass; true
    else elementClass == tag.getClass
