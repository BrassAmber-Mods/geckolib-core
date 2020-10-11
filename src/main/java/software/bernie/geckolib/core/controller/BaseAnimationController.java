/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.geckolib.core.controller;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.molang.MolangParser;
import software.bernie.geckolib.core.AnimationState;
import software.bernie.geckolib.core.IAnimatable;
import software.bernie.geckolib.core.IAnimatableModel;
import software.bernie.geckolib.core.PlayState;
import software.bernie.geckolib.core.builder.Animation;
import software.bernie.geckolib.core.builder.AnimationBuilder;
import software.bernie.geckolib.core.easing.EasingType;
import software.bernie.geckolib.core.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.event.ParticleKeyFrameEvent;
import software.bernie.geckolib.core.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.event.predicate.AnimationEvent;
import software.bernie.geckolib.core.keyframe.*;
import software.bernie.geckolib.core.processor.IBone;
import software.bernie.geckolib.core.snapshot.BoneSnapshot;

import java.util.*;
import java.util.function.Function;

/**
 * The type Animation controller.
 *
 * @param <T> the type parameter
 */
public abstract class BaseAnimationController<T extends IAnimatable>
{
	/**
	 * The Entity.
	 */
	protected T animatable;

	/**
	 * The name of the animation controller
	 */
	private String name;

	protected AnimationState animationState = AnimationState.Stopped;

	/**
	 * How long it takes to transition between animations
	 */
	public double transitionLengthTicks;

	/**
	 * The sound listener is called every time a sound keyframe is encountered (i.e. every frame)
	 */
	private ISoundListener soundListener;

	/**
	 * The particle listener is called every time a particle keyframe is encountered (i.e. every frame)
	 */
	private IParticleListener particleListener;


	/**
	 * The custom instruction listener is called every time a custom instruction keyframe is encountered (i.e. every frame)
	 */
	private ICustomInstructionListener customInstructionListener;

	public boolean isJustStarting = false;

	/**
	 * An AnimationPredicate is run every render frame for ever AnimationController. The "test" method is where you should change animations, stop animations, restart, etc.
	 */
	@FunctionalInterface
	public interface IAnimationPredicate<P>
	{
		/**
		 * An AnimationPredicate is run every render frame for ever AnimationController. The "test" method is where you should change animations, stop animations, restart, etc.
		 *
		 * @return TRUE if the animation should continue, FALSE if it should stop.
		 */
		<P extends IAnimatable> PlayState test(AnimationEvent<P> event);
	}

	/**
	 * Sound Listeners are run when a sound keyframe is hit. You can either return the SoundEvent and geckolib will play the sound for you, or return null and handle the sounds yourself.
	 */
	@FunctionalInterface
	public interface ISoundListener
	{
		/**
		 * Sound Listeners are run when a sound keyframe is hit. You can either return the SoundEvent and geckolib will play the sound for you, or return null and handle the sounds yourself.
		 */
		<ENTITY extends IAnimatable> void playSound(SoundKeyframeEvent<ENTITY> event);
	}

	/**
	 * Particle Listeners are run when a sound keyframe is hit. You need to handle the actual playing of the particle yourself.
	 */
	@FunctionalInterface
	public interface IParticleListener
	{
		/**
		 * Particle Listeners are run when a sound keyframe is hit. You need to handle the actual playing of the particle yourself.
		 */
		<ENTITY extends IAnimatable> void summonParticle(ParticleKeyFrameEvent<ENTITY> event);
	}

	/**
	 * Custom instructions can be added in blockbench by enabling animation effects in Animation - Animate Effects. You can then add custom instruction keyframes and use them as timecodes/events to handle in code.
	 */
	@FunctionalInterface
	public interface ICustomInstructionListener
	{
		/**
		 * Custom instructions can be added in blockbench by enabling animation effects in Animation - Animate Effects. You can then add custom instruction keyframes and use them as timecodes/events to handle in code.
		 */
		<ENTITY extends IAnimatable> void executeInstruction(CustomInstructionKeyframeEvent<ENTITY> event);
	}


	private final HashMap<String, BoneAnimationQueue> boneAnimationQueues = new HashMap<>();
	private double tickOffset = 0;
	protected Queue<Animation> animationQueue = new LinkedList<>();
	protected Animation currentAnimation;
	protected AnimationBuilder currentAnimationBuilder = new AnimationBuilder();
	protected boolean shouldResetTick = false;
	private HashMap<String, BoneSnapshot> boneSnapshots = new HashMap<>();
	private boolean justStopped = false;
	protected boolean justStartedTransition = false;
	public Function<Double, Double> customEasingMethod;
	protected boolean needsAnimationReload = false;

	public abstract void setAnimation( AnimationBuilder builder);


	/**
	 * By default Geckolib uses the easing types of every keyframe. If you want to override that for an entire AnimationController, change this value.
	 */
	public EasingType easingType = EasingType.NONE;


	/**
	 * Instantiates a new Animation controller. Each animation controller can run one animation at a time. You can have several animation controllers for each entity, i.e. one animation to control the entity's size, one to control movement, attacks, etc.
	 *
	 * @param animatable            The entity
	 * @param name                  Name of the animation controller (move_controller, size_controller, attack_controller, etc.)
	 * @param transitionLengthTicks How long it takes to transition between animations (IN TICKS!!)
	 */
	protected BaseAnimationController(T animatable, String name, float transitionLengthTicks)
	{
		this.animatable = animatable;
		this.name = name;
		this.transitionLengthTicks = transitionLengthTicks;
	}


	/**
	 * Instantiates a new Animation controller. Each animation controller can run one animation at a time. You can have several animation controllers for each entity, i.e. one animation to control the entity's size, one to control movement, attacks, etc.
	 *
	 * @param animatable            The entity
	 * @param name                  Name of the animation controller (move_controller, size_controller, attack_controller, etc.)
	 * @param transitionLengthTicks How long it takes to transition between animations (IN TICKS!!)
	 * @param easingtype            The method of easing to use. The other constructor defaults to no easing.
	 */
	public BaseAnimationController(T animatable, String name, float transitionLengthTicks, EasingType easingtype)
	{
		this.animatable = animatable;
		this.name = name;
		this.transitionLengthTicks = transitionLengthTicks;
		this.easingType = easingtype;
	}

	/**
	 * Instantiates a new Animation controller. Each animation controller can run one animation at a time. You can have several animation controllers for each entity, i.e. one animation to control the entity's size, one to control movement, attacks, etc.
	 *
	 * @param animatable            The entity
	 * @param name                  Name of the animation controller (move_controller, size_controller, attack_controller, etc.)
	 * @param transitionLengthTicks How long it takes to transition between animations (IN TICKS!!)
	 * @param customEasingMethod    If you want to use an easing method that's not included in the EasingType enum, pass your method into here. The parameter that's passed in will be a number between 0 and 1. Return a number also within 0 and 1. Take a look at {@link software.bernie.geckolib.core.easing.EasingManager}
	 */
	public BaseAnimationController(T animatable, String name, float transitionLengthTicks, Function<Double, Double> customEasingMethod)
	{
		this.animatable = animatable;
		this.name = name;
		this.transitionLengthTicks = transitionLengthTicks;
		this.customEasingMethod = customEasingMethod;
		this.easingType = EasingType.CUSTOM;
	}

	/**
	 * Gets the controller's name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gets the current animation. Can be null
	 *
	 * @return the current animation
	 */

	public Animation getCurrentAnimation()
	{
		return currentAnimation;
	}

	/**
	 * Returns the current state of this animation controller.
	 */
	public AnimationState getAnimationState()
	{
		return animationState;
	}


	/**
	 * Gets the current animation's bone animation queues.
	 *
	 * @return the bone animation queues
	 */
	public HashMap<String, BoneAnimationQueue> getBoneAnimationQueues()
	{
		return boneAnimationQueues;
	}

	/**
	 * Registers a sound listener.
	 */
	public void registerSoundListener(ISoundListener soundListener)
	{
		this.soundListener = soundListener;
	}

	/**
	 * Registers a particle listener.
	 */
	public void registerParticleListener(IParticleListener particleListener)
	{
		this.particleListener = particleListener;
	}

	/**
	 * Registers a custom instruction listener.
	 */
	public void registerCustomInstructionListener(ICustomInstructionListener customInstructionListener)
	{
		this.customInstructionListener = customInstructionListener;
	}


	/**
	 * This method is called every frame in order to populate the animation point queues, and process animation state logic.
	 *
	 * @param tick                   The current tick + partial tick
	 * @param event                  The animation test event
	 * @param modelRendererList      The list of all AnimatedModelRender's
	 * @param boneSnapshotCollection The bone snapshot collection
	 */
	public void process(double tick, AnimationEvent event, List<IBone> modelRendererList, HashMap<IBone, BoneSnapshot> boneSnapshotCollection, MolangParser parser, boolean crashWhenCantFindBone)
	{
		createInitialQueues(modelRendererList);

		double actualTick = tick;
		tick = adjustTick(tick);

		// Transition period has ended, reset the tick and set the animation to running
		if (animationState == AnimationState.Transitioning && tick >= transitionLengthTicks)
		{
			this.shouldResetTick = true;
			animationState = AnimationState.Running;
			tick = adjustTick(actualTick);
		}

		assert tick >= 0 : "GeckoLib: Tick was less than zero";

		// This tests the animation predicate
		PlayState playState = this.testAnimationPredicate(event);
		if (playState == PlayState.STOP || (currentAnimation == null && animationQueue.size() == 0))
		{
			// The animation should transition to the model's initial state
			animationState = AnimationState.Stopped;
			justStopped = true;
			return;
		}
		if (justStartedTransition && (shouldResetTick || justStopped))
		{
			justStopped = false;
			tick = adjustTick(actualTick);
		}
		else if(currentAnimation == null && this.animationQueue.size() != 0)
		{
			this.shouldResetTick = true;
			this.animationState = AnimationState.Transitioning;
			justStartedTransition = true;
			needsAnimationReload = false;
			tick = adjustTick(actualTick);
		}
		else
		{
			if (animationState != AnimationState.Transitioning)
			{
				animationState = AnimationState.Running;
			}
		}
		// Handle transitioning to a different animation (or just starting one)
		if (animationState == AnimationState.Transitioning)
		{
			// Just started transitioning, so set the current animation to the first one
			if (tick == 0 || isJustStarting)
			{
				justStartedTransition = false;
				this.currentAnimation = animationQueue.poll();
				resetEventKeyFrames(currentAnimation);
				saveSnapshotsForAnimation(currentAnimation, boneSnapshotCollection);
			}
			if (currentAnimation != null)
			{
				setAnimTime(parser, tick);
				for (BoneAnimation boneAnimation : currentAnimation.boneAnimations)
				{
					BoneAnimationQueue boneAnimationQueue = boneAnimationQueues.get(boneAnimation.boneName);
					BoneSnapshot boneSnapshot = this.boneSnapshots.get(boneAnimation.boneName);
					Optional<IBone> first = modelRendererList.stream().filter(
							x -> x.getName().equals(boneAnimation.boneName)).findFirst();
					if (!first.isPresent())
					{
						if (crashWhenCantFindBone)
						{
							throw new RuntimeException("Could not find bone: " + boneAnimation.boneName);
						}
						else
						{
							continue;
						}
					}
					BoneSnapshot initialSnapshot = first.get().getInitialSnapshot();
					assert boneSnapshot != null : "Bone snapshot was null";

					VectorKeyFrameList<KeyFrame<IValue>> rotationKeyFrames = boneAnimation.rotationKeyFrames;
					VectorKeyFrameList<KeyFrame<IValue>> positionKeyFrames = boneAnimation.positionKeyFrames;
					VectorKeyFrameList<KeyFrame<IValue>> scaleKeyFrames = boneAnimation.scaleKeyFrames;

					// Adding the initial positions of the upcoming animation, so the model transitions to the initial state of the new animation
					if (!rotationKeyFrames.xKeyFrames.isEmpty())
					{
						boneAnimationQueue.rotationXQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.rotationValueX - initialSnapshot.rotationValueX, rotationKeyFrames.xKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.rotationYQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.rotationValueY - initialSnapshot.rotationValueY, rotationKeyFrames.yKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.rotationZQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.rotationValueZ - initialSnapshot.rotationValueZ, rotationKeyFrames.zKeyFrames.get(0).getStartValue().get()));
					}

					if (!positionKeyFrames.xKeyFrames.isEmpty())
					{
						boneAnimationQueue.positionXQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.positionOffsetX, positionKeyFrames.xKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.positionYQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.positionOffsetY, positionKeyFrames.yKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.positionZQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.positionOffsetZ, positionKeyFrames.zKeyFrames.get(0).getStartValue().get()));
					}

					if (!scaleKeyFrames.xKeyFrames.isEmpty())
					{
						boneAnimationQueue.scaleXQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.scaleValueX, scaleKeyFrames.xKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.scaleYQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.scaleValueY, scaleKeyFrames.yKeyFrames.get(0).getStartValue().get()));
						boneAnimationQueue.scaleZQueue.add(new AnimationPoint(null, tick, transitionLengthTicks, boneSnapshot.scaleValueZ, scaleKeyFrames.zKeyFrames.get(0).getStartValue().get()));
					}
				}
			}
		}
		else if (getAnimationState() == AnimationState.Running)
		{
			// Actually run the animation
			processCurrentAnimation(tick, actualTick, parser, crashWhenCantFindBone);
		}
	}

	private void setAnimTime(MolangParser parser, double tick)
	{
		parser.setValue("query.anim_time", tick / 20);
	}

	protected abstract PlayState testAnimationPredicate(AnimationEvent<T> event);

	// At the beginning of a new transition, save a snapshot of the model's rotation, position, and scale values as the initial value to lerp from
	private void saveSnapshotsForAnimation(Animation animation, HashMap<IBone, BoneSnapshot> boneSnapshotCollection)
	{
		for (BoneSnapshot snapshot : boneSnapshotCollection.values())
		{
			if (animation != null && animation.boneAnimations != null)
			{
				if (animation.boneAnimations.stream().anyMatch(x -> x.boneName.equals(snapshot.name)))
				{
					this.boneSnapshots.put(snapshot.name, new BoneSnapshot(snapshot));
				}
			}
		}
	}

	private void processCurrentAnimation(double tick, double actualTick, MolangParser parser, boolean crashWhenCantFindBone)
	{
		assert currentAnimation != null;
		// Animation has ended
		if (tick >= currentAnimation.animationLength)
		{
			resetEventKeyFrames(currentAnimation);
			// If the current animation is set to loop, keep it as the current animation and just start over
			if (!currentAnimation.loop)
			{
				// Pull the next animation from the queue
				Animation peek = animationQueue.peek();
				if (peek == null)
				{
					// No more animations left, stop the animation controller
					this.animationState = AnimationState.Stopped;
					return;
				}
				else
				{
					// Otherwise, set the state to transitioning and start transitioning to the next animation next frame
					this.animationState = AnimationState.Transitioning;
					shouldResetTick = true;
				}
			}
			else
			{
				// Reset the adjusted tick so the next animation starts at tick 0
				shouldResetTick = true;
				tick = adjustTick(actualTick);
			}
		}
		setAnimTime(parser, tick);

		// Loop through every boneanimation in the current animation and process the values
		List<BoneAnimation> boneAnimations = currentAnimation.boneAnimations;
		for (BoneAnimation boneAnimation : boneAnimations)
		{
			BoneAnimationQueue boneAnimationQueue = boneAnimationQueues.get(boneAnimation.boneName);
			if (boneAnimationQueue == null)
			{
				if (crashWhenCantFindBone)
				{
					throw new RuntimeException("Could not find bone: " + boneAnimation.boneName);
				}
				else
				{
					continue;
				}
			}
			VectorKeyFrameList<KeyFrame<IValue>> rotationKeyFrames = boneAnimation.rotationKeyFrames;
			VectorKeyFrameList<KeyFrame<IValue>> positionKeyFrames = boneAnimation.positionKeyFrames;
			VectorKeyFrameList<KeyFrame<IValue>> scaleKeyFrames = boneAnimation.scaleKeyFrames;

			if (!rotationKeyFrames.xKeyFrames.isEmpty())
			{
				boneAnimationQueue.rotationXQueue.add(getAnimationPointAtTick(rotationKeyFrames.xKeyFrames, tick));
				boneAnimationQueue.rotationYQueue.add(getAnimationPointAtTick(rotationKeyFrames.yKeyFrames, tick));
				boneAnimationQueue.rotationZQueue.add(getAnimationPointAtTick(rotationKeyFrames.zKeyFrames, tick));
			}

			if (!positionKeyFrames.xKeyFrames.isEmpty())
			{
				boneAnimationQueue.positionXQueue.add(getAnimationPointAtTick(positionKeyFrames.xKeyFrames, tick));
				boneAnimationQueue.positionYQueue.add(getAnimationPointAtTick(positionKeyFrames.yKeyFrames, tick));
				boneAnimationQueue.positionZQueue.add(getAnimationPointAtTick(positionKeyFrames.zKeyFrames, tick));
			}

			if (!scaleKeyFrames.xKeyFrames.isEmpty())
			{
				boneAnimationQueue.scaleXQueue.add(getAnimationPointAtTick(scaleKeyFrames.xKeyFrames, tick));
				boneAnimationQueue.scaleYQueue.add(getAnimationPointAtTick(scaleKeyFrames.yKeyFrames, tick));
				boneAnimationQueue.scaleZQueue.add(getAnimationPointAtTick(scaleKeyFrames.zKeyFrames, tick));
			}
		}

		if (soundListener != null || particleListener != null || customInstructionListener != null)
		{
			for (EventKeyFrame<String> soundKeyFrame : currentAnimation.soundKeyFrames)
			{
				if (!soundKeyFrame.hasExecuted && tick >= soundKeyFrame.getStartTick())
				{
					SoundKeyframeEvent event = new SoundKeyframeEvent(this.animatable, tick, soundKeyFrame.getEventData(),
							this);
					soundListener.playSound(event);
					soundKeyFrame.hasExecuted = true;
				}
			}

			for (ParticleEventKeyFrame particleEventKeyFrame : currentAnimation.particleKeyFrames)
			{
				if (!particleEventKeyFrame.hasExecuted && tick >= particleEventKeyFrame.getStartTick())
				{
					ParticleKeyFrameEvent event = new ParticleKeyFrameEvent(this.animatable, tick,
							particleEventKeyFrame.effect, particleEventKeyFrame.locator, particleEventKeyFrame.script,
							this);
					particleListener.summonParticle(event);
					particleEventKeyFrame.hasExecuted = true;
				}
			}

			for (EventKeyFrame<List<String>> customInstructionKeyFrame : currentAnimation.customInstructionKeyframes)
			{
				if (!customInstructionKeyFrame.hasExecuted && tick >= customInstructionKeyFrame.getStartTick())
				{
					CustomInstructionKeyframeEvent event = new CustomInstructionKeyframeEvent(this.animatable, tick,
							customInstructionKeyFrame.getEventData(), this);
					customInstructionListener.executeInstruction(event);
					customInstructionKeyFrame.hasExecuted = true;
				}
			}
		}

		if (this.transitionLengthTicks == 0 && shouldResetTick && this.animationState == AnimationState.Transitioning)
		{
			this.currentAnimation = animationQueue.poll();
		}
	}

	//Helper method to populate all the initial animation point queues
	private void createInitialQueues(List<IBone> modelRendererList)
	{
		boneAnimationQueues.clear();
		for (IBone modelRenderer : modelRendererList)
		{
			boneAnimationQueues.put(modelRenderer.getName(), new BoneAnimationQueue(modelRenderer));
		}

	}

	// Used to reset the "tick" everytime a new animation starts, a transition starts, or something else of importance happens
	private double adjustTick(double tick)
	{
		if (shouldResetTick)
		{
			this.tickOffset = tick;
			shouldResetTick = false;
			return 0;
		}
		//assert tick - this.tickOffset >= 0;
		return (tick - this.tickOffset < 0 ? 0 : tick - this.tickOffset);
	}

	//Helper method to transform a KeyFrameLocation to an AnimationPoint
	private AnimationPoint getAnimationPointAtTick(List<KeyFrame<IValue>> frames, double tick)
	{
		KeyFrameLocation<KeyFrame<IValue>> location = getCurrentKeyFrameLocation(frames, tick);
		KeyFrame<IValue> currentFrame = location.currentFrame;
		return new AnimationPoint(currentFrame, location.currentTick, currentFrame.getLength(),
				currentFrame.getStartValue().get(),
				currentFrame.getEndValue().get());
	}

	/**
	 * Returns the current keyframe object, plus how long the previous keyframes have taken (aka elapsed animation time)
	 **/
	private KeyFrameLocation<KeyFrame<IValue>> getCurrentKeyFrameLocation(List<KeyFrame<IValue>> frames, double ageInTicks)
	{
		double totalTimeTracker = 0;
		for (int i = 0; i < frames.size(); i++)
		{
			KeyFrame frame = frames.get(i);
			totalTimeTracker += frame.getLength();
			if (totalTimeTracker > ageInTicks)
			{
				double tick = (ageInTicks - (totalTimeTracker - frame.getLength()));
				return new KeyFrameLocation<>(frame, tick);
			}
		}
		return new KeyFrameLocation(frames.get(frames.size() - 1), ageInTicks);
	}


	private void resetEventKeyFrames(Animation animation)
	{
		if (animation == null)
		{
			return;
		}
		if (!animation.soundKeyFrames.isEmpty())
		{
			for (EventKeyFrame soundKeyFrame : animation.soundKeyFrames)
			{
				soundKeyFrame.hasExecuted = false;
			}
		}
		if (!animation.particleKeyFrames.isEmpty())
		{
			for (EventKeyFrame particleKeyFrame : animation.particleKeyFrames)
			{
				particleKeyFrame.hasExecuted = false;
			}
		}
		if (!animation.customInstructionKeyframes.isEmpty())
		{
			for (EventKeyFrame customInstructionKeyFrame : animation.customInstructionKeyframes)
			{
				customInstructionKeyFrame.hasExecuted = false;
			}
		}
	}

	public void markNeedsReload()
	{
		this.needsAnimationReload = true;
	}

	public void clearAnimationCache()
	{
		this.currentAnimationBuilder = new AnimationBuilder();
	}
}