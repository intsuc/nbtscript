package nbts

import java.util.{AbstractList, ArrayList, HashMap, List, Map}
import org.apache.commons.lang3.ArrayUtils

sealed trait Tag:
  val id: Byte

object EndTag extends Tag:
  val id: Byte = 0

final case class StringTag private (data: String) extends Tag:
  val id: Byte = 8

object StringTag:
  val Empty = StringTag("")
  def apply(data: String): StringTag = if data.isEmpty then Empty else new StringTag(data)

final case class CompoundTag private (data: Map[String, Tag]) extends Tag:
  val id: Byte = 10

object CompoundTag:
  def apply(): CompoundTag = new CompoundTag(HashMap())

sealed trait NumericTag extends Tag:
  def asByte: Byte
  def asShort: Short
  def asInt: Int
  def asLong: Long
  def asFloat: Float
  def asDouble: Double
  def asNumber: Number

final case class ByteTag private (data: Byte) extends NumericTag:
  val id: Byte = 1
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
  val id: Byte = 2
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
  val id: Byte = 3
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
  val id: Byte = 4
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
  val id: Byte = 5
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = data.toLong
  def asFloat: Float = data
  def asDouble: Double = data
  def asNumber: Number = data

object FloatTag:
  val ZERO: FloatTag = FloatTag(0.0f)
  def apply(data: Float): FloatTag = if data == 0.0f then ZERO else new FloatTag(data)

final case class DoubleTag private (data: Double) extends NumericTag:
  val id: Byte = 6
  def asByte: Byte = (floor(data) & 0xff).toByte
  def asShort: Short = (floor(data) & 0xffff).toShort
  def asInt: Int = floor(data)
  def asLong: Long = Math.floor(data).toLong
  def asFloat: Float = data.toFloat
  def asDouble: Double = data
  def asNumber: Number = data

object DoubleTag:
  val ZERO: DoubleTag = DoubleTag(0.0)
  def apply(data: Double): DoubleTag = if data == 0.0 then ZERO else new DoubleTag(data)

sealed abstract class CollectionTag[T <: Tag] extends AbstractList[T] with Tag:
  def set(index: Int, element: T): T
  def add(index: Int, element: T): Unit
  def remove(index: Int): T
  def setTag(index: Int, tag: Tag): Boolean
  def addTag(index: Int, tag: Tag): Boolean

final case class ByteArrayTag(private var data: Array[Byte]) extends CollectionTag[ByteTag]:
  val id: Byte = 7

  def size: Int = data.size

  def get(index: Int): ByteTag = ByteTag(data(index))

  override def set(index: Int, element: ByteTag): ByteTag =
    val old = data(index)
    data(index) = element.asByte
    ByteTag(old)

  override def add(index: Int, element: ByteTag): Unit =
    data = ArrayUtils.insert(index, data, element.asByte)

  override def remove(index: Int): ByteTag =
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
  val id: Byte = 11

  def size: Int = data.size

  def get(index: Int): IntTag = IntTag(data(index))

  override def set(index: Int, element: IntTag): IntTag =
    val old = data(index)
    data(index) = element.asInt
    IntTag(old)

  override def add(index: Int, element: IntTag): Unit =
    data = ArrayUtils.insert(index, data, element.asInt)

  override def remove(index: Int): IntTag =
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
  val id: Byte = 12

  def size: Int = data.size

  def get(index: Int): LongTag = LongTag(data(index))

  override def set(index: Int, element: LongTag): LongTag =
    val old = data(index)
    data(index) = element.asLong
    LongTag(old)

  override def add(index: Int, element: LongTag): Unit =
    data = ArrayUtils.insert(index, data, element.asLong)

  override def remove(index: Int): LongTag =
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

final case class ListTag private (data: List[Tag], var elementId: Byte) extends CollectionTag[Tag]:
  val id: Byte = 9

  def apply(): ListTag = ListTag(ArrayList(), 0)

  def size: Int = data.size

  def get(index: Int): Tag = data.get(index)

  override def set(index: Int, element: Tag): Tag =
    val old = data.get(index)
    if setTag(index, element) then old
    else throw UnsupportedOperationException(s"Trying to add tag of type ${element.id} to list of $elementId")

  override def add(index: Int, element: Tag): Unit =
    if !addTag(index, element) then throw UnsupportedOperationException(s"Trying to add tag of type ${element.id} to list of $elementId")

  override def remove(index: Int): Tag =
    val old = data.remove(index)
    if data.isEmpty then elementId = 0
    old

  def setTag(index: Int, tag: Tag): Boolean =
    if !updateId(tag) then false
    else data.set(index, tag); true

  def addTag(index: Int, tag: Tag): Boolean =
    if !updateId(tag) then false
    else data.add(index, tag); true

  private def updateId(tag: Tag): Boolean =
    if tag.id == 0 then false
    else if elementId == 0 then
      elementId = tag.id; true
    else elementId == tag.id
