package fi.dy.masa.underphangables;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class AttackEntityEventHandler
{
    @SubscribeEvent
    public void onAttackEntityEvent(AttackEntityEvent event)
    {
        if (event.entityPlayer.capabilities.isCreativeMode == true ||
            event.target.isEntityInvulnerable() == true)
        {
            return;
        }

        if (event.target instanceof EntityItemFrame)
        {
            EntityItemFrame entityItemFrame = (EntityItemFrame)event.target;
            ItemStack stack = entityItemFrame.getDisplayedItem();

            if (event.target.worldObj.isRemote == false)
            {
                if (stack != null)
                {
                    dropDisplayItemFromItemFrame(entityItemFrame, stack, getEntityYawFacing(event.target));
                    entityItemFrame.setDisplayedItem(null);
                }
                else
                {
                    dropItemWithAdjustedPosition(new ItemStack(Items.item_frame), entityItemFrame, getEntityYawFacing(event.target));
                    entityItemFrame.setDead();
                }
            }

            event.setCanceled(true);
        }
        else if (event.target instanceof EntityPainting)
        {
            if (event.target.worldObj.isRemote == false)
            {
                dropItemWithAdjustedPosition(new ItemStack(Items.painting), event.target, getEntityYawFacing(event.target));

                event.target.setDead();
            }

            event.setCanceled(true);
        }
    }

    public static ForgeDirection getEntityYawFacing(Entity entity)
    {
        byte yawToDir[] = {2, 5, 3, 4};
        int yaw = MathHelper.floor_double((double)(entity.rotationYaw * 4.0f / 360.0f) + 0.5d) & 3;

        ForgeDirection facing = ForgeDirection.getOrientation(yawToDir[yaw]);

        if (facing == ForgeDirection.EAST || facing == ForgeDirection.WEST)
        {
            // Because wtf @ EntityHanging#setDirection()
            facing = facing.getOpposite();
        }

        return facing;
    }

    public static EntityItem createEntityItemWithoutHorizontalMotion(ItemStack stack, World world, double x, double y, double z, double motionY)
    {
        EntityItem entityItem = new EntityItem(world, x, y, z, stack);
        entityItem.motionX = 0.0d;
        entityItem.motionY = motionY;
        entityItem.motionZ = 0.0d;

        entityItem.delayBeforeCanPickup = 10;

        return entityItem;
    }

    public static void dropItemWithAdjustedPosition(ItemStack stack, Entity entity, ForgeDirection facingToAdjustTo)
    {
        double posX = entity.posX + facingToAdjustTo.offsetX * 0.15f;
        double posZ = entity.posZ + facingToAdjustTo.offsetZ * 0.15f;
        EntityItem entityItem = createEntityItemWithoutHorizontalMotion(stack, entity.worldObj, posX, entity.posY, posZ, 0.1d);

        //System.out.println("adjusting towards " + facingToAdjustTo);
        //System.out.println(String.format("post x: %.2f y: %.2f z: %.2f", entityItem.posX, entityItem.posY, entityItem.posZ));

        if (entity.captureDrops)
        {
            entity.capturedDrops.add(entityItem);
        }
        else
        {
            entity.worldObj.spawnEntityInWorld(entityItem);
        }
    }

    public static void dropDisplayItemFromItemFrame(EntityItemFrame entityItemFrame, ItemStack stack, ForgeDirection facing)
    {
        float dropChance = entityItemFrame.itemDropChance;

        if (stack != null && stack.stackSize > 0 && stack.getItem() != null && entityItemFrame.worldObj.rand.nextFloat() < dropChance)
        {
            stack = stack.copy();
            entityItemFrame.removeFrameFromMap(stack);
            dropItemWithAdjustedPosition(stack, entityItemFrame, facing);
        }
    }
}
