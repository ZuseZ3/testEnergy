package org.jsonbuddy.JPowerMonitor;

import com.sun.tools.attach.VirtualMachine;
import org.junit.jupiter.api.extension.*;
import java.lang.management.ManagementFactory;

import static org.hamcrest.MatcherAssert.assertThat;

public class JPowerMonitor implements BeforeAllCallback, AfterAllCallback {

    VirtualMachine vm;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
        vm = VirtualMachine.attach(pid);

        try {
            vm.loadAgent("joularjx-1.5.jar", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        vm.detach();
        Process p = Runtime.getRuntime().exec("python3 report.py");
    }

}
