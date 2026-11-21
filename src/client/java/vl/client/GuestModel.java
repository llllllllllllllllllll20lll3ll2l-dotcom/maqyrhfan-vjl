package vl.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель Гостя: гуманоидный скелет с канонической раскладкой текстуры 64x64,
 * который рендерер растягивает по вертикали.
 */
public class GuestModel extends EntityModel<GuestRenderState> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public GuestModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Голова — лицо в области (8, 8) текстуры.
		root.addOrReplaceChild(
			"head",
			CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 0.0F, 0.0F)
		);

		root.addOrReplaceChild(
			"body",
			CubeListBuilder.create()
				.texOffs(16, 16)
				.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(0.0F, 0.0F, 0.0F)
		);

		root.addOrReplaceChild(
			"right_arm",
			CubeListBuilder.create()
				.texOffs(40, 16)
				.addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-5.0F, 2.0F, 0.0F)
		);
		root.addOrReplaceChild(
			"left_arm",
			CubeListBuilder.create()
				.texOffs(32, 48)
				.addBox(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(5.0F, 2.0F, 0.0F)
		);

		root.addOrReplaceChild(
			"right_leg",
			CubeListBuilder.create()
				.texOffs(0, 16)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(-2.0F, 12.0F, 0.0F)
		);
		root.addOrReplaceChild(
			"left_leg",
			CubeListBuilder.create()
				.texOffs(16, 48)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
			PartPose.offset(2.0F, 12.0F, 0.0F)
		);

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(GuestRenderState state) {
		super.setupAnim(state);

		float breathe = Mth.sin(state.ageInTicks * 0.05F) * 0.04F;

		// Голова: медленно водит из стороны в сторону, будто прислушивается к тебе.
		this.head.yRot = Mth.sin(state.ageInTicks * 0.035F) * 0.5F + state.sway;
		this.head.xRot = -0.28F + Mth.cos(state.ageInTicks * 0.023F) * 0.1F;
		this.head.zRot = Mth.sin(state.ageInTicks * 0.017F) * 0.12F;

		this.body.xRot = 0.12F + breathe;

		// Руки чуть впереди корпуса — он всегда готов дотронуться.
		float swing = Mth.cos(state.walkAnimationPos * 0.45F) * 0.28F;
		this.rightArm.xRot = -0.5F - swing;
		this.leftArm.xRot = -0.5F + swing;
		this.rightArm.zRot = 0.16F;
		this.leftArm.zRot = -0.16F;

		this.rightLeg.xRot = swing * 0.9F;
		this.leftLeg.xRot = -swing * 0.9F;
	}
}
