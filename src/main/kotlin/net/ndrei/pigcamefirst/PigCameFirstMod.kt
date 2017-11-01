package net.ndrei.pigcamefirst

import net.minecraft.entity.ai.EntityAIBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityPig
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagByte
import net.minecraftforge.common.util.Constants
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLConstructionEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Created by CF on 2017-06-28.
 */
@Mod(modid = MOD_ID, version = MOD_VERSION, name = MOD_NAME,
    acceptedMinecraftVersions = MOD_MC_VERSION,
    dependencies = MOD_DEPENDENCIES,
    useMetadata = true, modLanguage = "kotlin", modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter"
    )
@Mod.EventBusSubscriber
object PigCameFirstMod {
    lateinit var logger: Logger

    @Mod.EventHandler
    fun construct(event: FMLConstructionEvent) {
        this.logger = LogManager.getLogger(Loader.instance().activeModContainer()!!.modId)
    }

    @SubscribeEvent
    @JvmStatic
    fun onEntityJoin(event: EntityJoinWorldEvent) {
        if (event.entity is EntityPig) {
            val pig = event.entity as EntityPig
            pig.tasks.addTask(42, PigEggTask(pig))
        }
        else if (event.entity is EntityItem) {
            val ei = event.entity as EntityItem
            if ((ei.item.item === Items.EGG) && !ei.item.hasTagCompound() && (ei.item.tagCompound?.getByte("pig_came_first") != 1.toByte())) {
                ei.setDead()
                ei.item = ItemStack.EMPTY
            }
        }
    }

    private class PigEggTask(private val pig: EntityPig) : EntityAIBase() {
        private var timeUntilNextEgg: Int
            get() = if (this.pig.entityData.hasKey("egg_timer", Constants.NBT.TAG_INT))
                this.pig.entityData.getInteger("egg_timer")
            else this.getNewTimer().also { this.timeUntilNextEgg = it }
        set(value) = this.pig.entityData.setInteger("egg_timer", value)

        override fun shouldExecute() =
            (!this.pig.world.isRemote && !this.pig.isChild && !this.pig.isBeingRidden && (--this.timeUntilNextEgg <= 0))

        override fun shouldContinueExecuting() = false

        override fun startExecuting() {
            this.pig.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0f, (this.pig.world.rand.nextFloat() - this.pig.world.rand.nextFloat()) * 0.2f + 1.0f)
            this.pig.entityDropItem(ItemStack(Items.EGG, 1, 0).also {
                it.setTagInfo("pig_came_first", NBTTagByte(1))
            }, 0.0f)
            this.timeUntilNextEgg = this.getNewTimer()
        }

        private fun getNewTimer() = this.pig.world.rand.nextInt(6000) + 6000
    }
}
