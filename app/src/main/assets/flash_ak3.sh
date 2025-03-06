#!/system/bin/sh

## setup for testing:
unzip -p $Z tools*/busybox > $F/busybox;
unzip -p $Z META-INF/com/google/android/update-binary > $F/update-binary;
##

chmod 755 $F/busybox;
$F/busybox chmod 755 $F/update-binary;
$F/busybox chown root:root $F/busybox $F/update-binary;

TMP=$F/tmp;

$F/busybox rm -rf $TMP 2>/dev/null;
$F/busybox mkdir -p $TMP;
## $F/busybox sed -i "/export ZIPFILE=\"\$3\";/a export STATE=\"\$4\";\nexport SLOT=\"\$5\";" $F/update-binary;
## $F/busybox sed -i 's/\[ -e \/dev\/block\/$byname\/system \] || slot=\$(find_slot);/[ -e \/dev\/block\/$byname\/system ] || slot=$SLOT;/' $F/update-binary;
## $F/busybox sed -i '/setup_env;/i sed -i "/is_slot_device=auto/i slot_select=$4" anykernel.sh' $F/update-binary;
## $F/busybox sed -i '/setup_env;/i sed -i '\''s/is_slot_device=auto/is_slot_device=1/'\'' anykernel.sh' $F/update-binary;

# update-binary <RECOVERY_API_VERSION> <OUTFD> <ZIPFILE>
AKHOME=$TMP/anykernel $F/busybox ash $F/update-binary 3 1 "$Z" "$S" "$P";
RC=$?;

$F/busybox rm -rf $TMP;
$F/busybox mount -o ro,remount -t auto /;
$F/busybox rm -f $F/update-binary $F/busybox;

# work around libsu not cleanly accepting return or exit as last line
safereturn() { return $RC; }
safereturn;
