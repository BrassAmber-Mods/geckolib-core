/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.geckolib.core.event;
import software.bernie.geckolib.core.controller.BaseAnimationController;

public class AnimationEvent<T>
{
	private final T entity;
	private final double animationTick;
	private final BaseAnimationController controller;
	/**
	 * This stores all the fields that are needed in the AnimationTestEvent
	 *
	 * @param entity          the entity
	 * @param animationTick   The amount of ticks that have passed in either the current transition or animation, depending on the controller's AnimationState.
	 * @param controller      the controller
	 */
	public AnimationEvent(T entity, double animationTick, BaseAnimationController controller)
	{
		this.entity = entity;
		this.animationTick = animationTick;
		this.controller = controller;
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
}
