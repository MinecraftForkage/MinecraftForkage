package squeek.applecore.api;

import net.minecraftforkage.internal.AppleCoreAccessor;
import net.minecraftforkage.internal.AppleCoreDispatcher;
import net.minecraftforkage.internal.AppleCoreMutator;

/**
 * Used to access/mutate various hidden values of the hunger system or fire standard AppleCore events.
 * 
 * See {@link IAppleCoreAccessor}, {@link IAppleCoreMutator}, and {@link IAppleCoreDispatcher} for a list of the available functions.
 * {@link #accessor}, {@link #mutator}, and {@link #dispatcher} will be initialized by AppleCore on startup.
 */
public abstract class AppleCoreAPI
{
	public static IAppleCoreAccessor accessor = new AppleCoreAccessor();
	public static IAppleCoreMutator mutator = new AppleCoreMutator();
	public static IAppleCoreDispatcher dispatcher = new AppleCoreDispatcher();
}
