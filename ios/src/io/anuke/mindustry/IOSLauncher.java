package io.anuke.mindustry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import io.anuke.kryonet.DefaultThreadImpl;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.core.ThreadHandler;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Unit;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UIApplicationOpenURLOptions;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static io.anuke.mindustry.Vars.ui;

public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        Unit.dp.addition -= 0.2f;

        Platform.instance = new Platform() {
            DateFormat format = SimpleDateFormat.getDateTimeInstance();

            @Override
            public String format(Date date) {
                return format.format(date);
            }

            @Override
            public String format(int number) {
                return NumberFormat.getIntegerInstance().format(number);
            }

            @Override
            public void addDialog(TextField field) {
                TextFieldDialogListener.add(field, 16);
            }

            @Override
            public void addDialog(TextField field, int maxLength) {
                TextFieldDialogListener.add(field, maxLength);
            }

            @Override
            public String getLocaleName(Locale locale) {
                return locale.getDisplayName(locale);
            }

            @Override
            public ThreadHandler.ThreadProvider getThreadProvider() {
                return new DefaultThreadImpl();
            }

            @Override
            public void shareImage(Pixmap image){
                //todo share it

            }
        };

        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new Mindustry(), config);
    }

    @Override
    public boolean openURL(UIApplication app, NSURL url, UIApplicationOpenURLOptions options) {
        System.out.println("Opened URL: " + url.getAbsoluteString());
        openURL(url);
        return false;
    }

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options) {
        boolean b = super.didFinishLaunching(application, options);

        if(options != null && options.has(UIApplicationLaunchOptions.Keys.URL())){
            System.out.println("Opened URL at launch: " + ((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())).getAbsoluteString());
            openURL(((NSURL)options.get(UIApplicationLaunchOptions.Keys.URL())));
        }

        return b;
    }

    void openURL(NSURL url){
        Timers.runTask(30f, () -> {
            if(!ui.editor.isShown()){
                ui.editor.show();
            }
            ui.editor.tryLoadMap(Gdx.files.absolute(url.getAbsoluteString()));
        });
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}