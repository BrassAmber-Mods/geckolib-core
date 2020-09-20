package software.bernie.geckolib.core.event.predicate;

import software.bernie.geckolib.core.IAnimatable;
import software.bernie.geckolib.core.controller.BaseAnimationController;


public class AnimationTestPredicate<T extends IAnimatable>
{
	private final T entity;
	public double animationTick;
	private final float limbSwing;
	private final float limbSwingAmount;
	private final float partialTick;
	private final boolean isMoving;

	protected BaseAnimationController controller;

	public AnimationTestPredicate(T entity, float limbSwing, float limbSwingAmount, float partialTick, boolean isMoving)
	{
		this.entity = entity;
		this.limbSwing = limbSwing;
		this.limbSwingAmount = limbSwingAmount;
		this.partialTick = partialTick;
		this.isMoving = isMoving;
	}

	/**
	 * Gets the amount of ticks that have passed in either the current transition or animation, depending on the controller's AnimationState.
	 *
	 * @return the animation tick
	 */
	public double getAnimationTick()
	{
		return animationTick;
	}

	public T getEntity()
	{
		return entity;
	}

	public BaseAnimationController getController()
	{
		return controller;
	}

	public void setController(BaseAnimationController controller)
	{
		this.controller = controller;
	}


	public float getLimbSwing()
	{
		return limbSwing;
	}
	public float getLimbSwingAmount()
	{
		return limbSwingAmount;
	}
	public float getPartialTick()
	{
		return partialTick;
	}
	public boolean isMoving()
	{
		return isMoving;
	}
}
