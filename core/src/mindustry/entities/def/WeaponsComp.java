package mindustry.entities.def;

import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

@Component
abstract class WeaponsComp implements Teamc, Posc, Rotc{
    transient float x, y, rotation;

    /** minimum cursor distance from player, fixes 'cross-eyed' shooting */
    static final float minAimDst = 20f;
    /** temporary weapon sequence number */
    static int sequenceNum = 0;

    /** weapon mount array, never null */
    @ReadOnly WeaponMount[] mounts = {};

    void setupWeapons(UnitDef def){
        mounts = new WeaponMount[def.weapons.size];
        for(int i = 0; i < mounts.length; i++){
            mounts[i] = new WeaponMount(def.weapons.get(i));
        }
    }

    /** Aim at something. This will make all mounts point at it. */
    void aim(Unitc unit, float x, float y){
        Tmp.v1.set(x, y).sub(this.x, this.y);
        if(Tmp.v1.len() < minAimDst) Tmp.v1.setLength(minAimDst);

        x = Tmp.v1.x + this.x;
        y = Tmp.v1.y + this.y;

        for(WeaponMount mount : mounts){
            mount.aimX = x;
            mount.aimY = y;
        }
    }

    /** Update shooting and rotation for this unit. */
    @Override
    public void update(){
        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;
            mount.reload = Math.max(mount.reload - Time.delta(), 0);

            if(mount.shoot){
                float rotation = this.rotation - 90;

                //rotate if applicable
                if(weapon.rotate){
                    float axisXOffset = weapon.mirror ? 0f : weapon.x;
                    float axisX = this.x + Angles.trnsx(rotation, axisXOffset, weapon.y),
                    axisY = this.y + Angles.trnsy(rotation, axisXOffset, weapon.y);

                    mount.rotation = Angles.moveToward(mount.rotation, Angles.angle(axisX, axisY, mount.aimX, mount.aimY), weapon.rotateSpeed);
                }

                //shoot if applicable
                //TODO only shoot if angle is reached, don't shoot inaccurately
                if(mount.reload <= 0.0001f){
                    for(int i : (weapon.mirror && !weapon.alternate ? Mathf.signs : Mathf.one)){
                        i *= Mathf.sign(weapon.flipped) * Mathf.sign(mount.side);

                        //m a t h
                        float weaponRotation = rotation + (weapon.rotate ? mount.rotation : 0);
                        float mountX = this.x + Angles.trnsx(rotation, weapon.x * i, weapon.y),
                        mountY = this.y + Angles.trnsy(rotation, weapon.x * i, weapon.y);
                        float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX * i, weapon.shootY),
                        shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX * i, weapon.shootY);
                        float shootAngle = weapon.rotate ? weaponRotation : Angles.angle(shootX, shootY, mount.aimX, mount.aimY);

                        shoot(weapon, shootX, shootY, shootAngle);
                    }

                    mount.side = !mount.side;
                    mount.reload = weapon.reload;
                }
            }
        }
    }

    private void shoot(Weapon weapon, float x, float y, float rotation){
        float baseX = this.x, baseY = this.y;

        weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

        sequenceNum = 0;
        if(weapon.shotDelay > 0.01f){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay, () -> bullet(weapon, x + this.x - baseX, y + this.y - baseY, f + Mathf.range(weapon.inaccuracy)));
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> bullet(weapon, x, y, f + Mathf.range(weapon.inaccuracy)));
        }

        BulletType ammo = weapon.bullet;

        Tmp.v1.trns(rotation + 180f, ammo.recoil);

        if(this instanceof Velc){
            //TODO apply force?
            ((Velc)this).vel().add(Tmp.v1);
        }

        Tmp.v1.trns(rotation, 3f);
        boolean parentize = ammo.keepVelocity;

        Effects.shake(weapon.shake, weapon.shake, x, y);
        weapon.ejectEffect.at(x, y, rotation);
        ammo.shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
        ammo.smokeEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
    }

    private void bullet(Weapon weapon, float x, float y, float angle){
        Tmp.v1.trns(angle, 3f);
        weapon.bullet.create(this, team(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd));
    }
}
