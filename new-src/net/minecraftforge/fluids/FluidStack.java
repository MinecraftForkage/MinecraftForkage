
package net.minecraftforge.fluids;

import java.util.Locale;

import com.google.common.base.Strings;

import cpw.mods.fml.common.registry.RegistryDelegate;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * ItemStack substitute for Fluids.
 *
 * NOTE: Equality is based on the Fluid, not the amount. Use
 * {@link #isFluidStackIdentical(FluidStack)} to determine if FluidID, Amount and NBT Tag are all
 * equal.
 *
 * @author King Lemming, SirSengir (LiquidStack)
 *
 */
public class FluidStack
{
    public int amount;
    public NBTTagCompound tag;
    private RegistryDelegate<Fluid> fluidDelegate;

    public FluidStack(Fluid fluid, int amount)
    {
        if (fluid == null)
        {
            throw new IllegalArgumentException("Cannot create a fluidstack from a null fluid.");
        }
        else if (!FluidRegistry.isFluidRegistered(fluid))
        {
            throw new IllegalArgumentException("Cannot create a fluidstack from an unregistered fluid "+fluid.getName()+" (type "+fluid.getClass().getName()+")");
        }
    	this.fluidDelegate = FluidRegistry.makeDelegate(fluid);
        this.amount = amount;
    }

    public FluidStack(Fluid fluid, int amount, NBTTagCompound nbt)
    {
        this(fluid, amount);

        if (nbt != null)
        {
            tag = (NBTTagCompound) nbt.copy();
        }
    }

    public FluidStack(FluidStack stack, int amount)
    {
        this(stack.getFluid(), amount, stack.tag);
    }

    @Deprecated // Use Fluid instead of fluid ID
    public FluidStack(int fluidID, int amount)
    {
    	this(FluidRegistry.getFluid(fluidID), amount);
    }

    @Deprecated // Use Fluid instead of fluid ID
    public FluidStack(int fluidID, int amount, NBTTagCompound nbt)
    {
    	this(FluidRegistry.getFluid(fluidID), amount, nbt);
    }

    /**
     * This provides a safe method for retrieving a FluidStack - if the Fluid is invalid, the stack
     * will return as null.
     */
    public static FluidStack loadFluidStackFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null)
        {
            return null;
        }
        String fluidName = nbt.getString("FluidName");

        if (fluidName == null || FluidRegistry.getFluid(fluidName) == null)
        {
            return null;
        }
        FluidStack stack = new FluidStack(FluidRegistry.getFluid(fluidName), nbt.getInteger("Amount"));

        if (nbt.hasKey("Tag"))
        {
            stack.tag = nbt.getCompoundTag("Tag");
        }
        return stack;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt.setString("FluidName", FluidRegistry.getFluidName(getFluid()));
        nbt.setInteger("Amount", amount);

        if (tag != null)
        {
            nbt.setTag("Tag", tag);
        }
        return nbt;
    }

    /** TODO why is there no setFluid method? */
    public final Fluid getFluid()
    {
        return fluidDelegate.get();
    }

    public String getLocalizedName()
    {
        return this.getFluid().getLocalizedName(this);
    }

    public String getUnlocalizedName()
    {
        return this.getFluid().getUnlocalizedName(this);
    }

    /**
     * @return A copy of this FluidStack
     */
    public FluidStack copy()
    {
        return new FluidStack(getFluid(), amount, tag);
    }

    /**
     * Determines if the FluidIDs and NBT Tags are equal. This does not check amounts.
     *
     * @param other
     *            The FluidStack for comparison
     * @return true if the Fluids (IDs and NBT Tags) are the same
     */
    public boolean isFluidEqual(FluidStack other)
    {
        return other != null && getFluid() == other.getFluid() && isFluidStackTagEqual(other);
    }

    private boolean isFluidStackTagEqual(FluidStack other)
    {
        return tag == null ? other.tag == null : other.tag == null ? false : tag.equals(other.tag);
    }

    /**
     * Determines if the NBT Tags are equal. Useful if the FluidIDs are known to be equal.
     */
    public static boolean areFluidStackTagsEqual(FluidStack stack1, FluidStack stack2)
    {
        return stack1 == null && stack2 == null ? true : stack1 == null || stack2 == null ? false : stack1.isFluidStackTagEqual(stack2);
    }

    /**
     * Determines if the Fluids are equal and this stack is larger.
     *
     * @param other
     * @return true if this FluidStack contains the other FluidStack (same fluid and >= amount)
     */
    public boolean containsFluid(FluidStack other)
    {
        return isFluidEqual(other) && amount >= other.amount;
    }

    /**
     * Determines if the FluidIDs, Amounts, and NBT Tags are all equal.
     *
     * @param other
     *            - the FluidStack for comparison
     * @return true if the two FluidStacks are exactly the same
     */
    public boolean isFluidStackIdentical(FluidStack other)
    {
        return isFluidEqual(other) && amount == other.amount;
    }

    /**
     * Determines if the FluidIDs and NBT Tags are equal compared to a registered container
     * ItemStack. This does not check amounts.
     *
     * @param other
     *            The ItemStack for comparison
     * @return true if the Fluids (IDs and NBT Tags) are the same
     */
    public boolean isFluidEqual(ItemStack other)
    {
        if (other == null)
        {
            return false;
        }

        if (other.getItem() instanceof IFluidContainerItem)
        {
            return isFluidEqual(((IFluidContainerItem) other.getItem()).getFluid(other));
        }

        return isFluidEqual(FluidContainerRegistry.getFluidForFilledItem(other));
    }

    @Override
    public final int hashCode()
    {
    	int code = 1;
    	code = 31*code + getFluid().hashCode();
    	code = 31*code + amount;
    	if (tag != null)
    		code = 31*code + tag.hashCode();
    	return code;
    }

    /**
     * Default equality comparison for a FluidStack. Same functionality as isFluidEqual().
     *
     * This is included for use in data structures.
     */
    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof FluidStack))
        {
            return false;
        }

        return isFluidEqual((FluidStack) o);
    }
}
