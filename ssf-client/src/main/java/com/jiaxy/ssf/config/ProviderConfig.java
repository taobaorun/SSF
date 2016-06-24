package com.jiaxy.ssf.config;

import com.jiaxy.ssf.common.Assert;
import com.jiaxy.ssf.common.ProtocolType;
import com.jiaxy.ssf.common.bo.SSFURL;
import com.jiaxy.ssf.exception.IllegalConfigureException;
import com.jiaxy.ssf.exception.InitException;
import com.jiaxy.ssf.intercept.MessageInvocation;
import com.jiaxy.ssf.intercept.MessageInvocationFactory;
import com.jiaxy.ssf.processor.ProcessorManagerFactory;
import com.jiaxy.ssf.processor.ProviderProcessor;
import com.jiaxy.ssf.regcenter.client.RegClient;
import com.jiaxy.ssf.regcenter.client.RegClientFactory;
import com.jiaxy.ssf.server.ProviderManager;
import com.jiaxy.ssf.util.Callbacks;
import com.jiaxy.ssf.util.SSFContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Title: <br>
 * <p>
 * Description: <br>
 * </p>
 *
 * @author <a href=mailto:taobaorun@gmail.com>wutao</a>
 *
 * @since 2016/04/07 17:49
 */
public class ProviderConfig<T> extends SSFConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProviderConfig.class);

    private T ref;

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }


    @Override
    public Class<?> getProxyClass() {
        return getServiceInterfaceClass();
    }

    public void doExport(ServerConfig serverConfig) {
        ProviderConfig exported = ProviderManager.getExportedProvider(this);
        String uniqueKey = buildUniqueKey();
        if ( exported != null ){
            logger.warn("duplicate provider config:{},check it",uniqueKey);
            return;
        } else {
            synchronized (this){
                Assert.notNull(alias,"alias is empty");
                Assert.notNull(serviceInterfaceName,"serviceInterfaceName is empty");
                Assert.notNull(ref,String.format("the implement of the %s instance is null",serviceInterfaceName));
                Class serviceClz = getProxyClass();
                if (!serviceClz.isInstance(ref)){
                    throw new IllegalConfigureException(String.format("%s is not instance of interface %s.please check ref",
                            ref.getClass().getName(),
                            serviceInterfaceName));
                }
                //callback info
                Callbacks.callbackInfoRegister(serviceClz);
                MessageInvocation invocation = MessageInvocationFactory.getMessageInvocation(this);
                ProviderProcessor providerProcessor = new ProviderProcessor(invocation);
                //register processor for this interface service
                ProcessorManagerFactory.getInstance().register(ProcessorManagerFactory.processorKey(serviceInterfaceName,alias),providerProcessor);
                ProviderManager.addExportedProvider(this);
                registerToRegCenter(serverConfig);
                logger.info("export provider [{}] successfully.",uniqueKey);
            }
        }
    }


    public void unExport(ServerConfig serverConfig){
        if ( ProviderManager.getExportedProvider(this) == null ){
            return;
        }
        unRegisterFromRegCenter(serverConfig);
        String uniqueKey = buildUniqueKey();
        ProcessorManagerFactory.getInstance().unRegister(serviceInterfaceName);
        ProviderManager.removeExportedProvider(this);
        logger.info("unExport provider [%s] successfully.",uniqueKey);
    }


    public void registerToRegCenter(ServerConfig serverConfig){
        List<RegistryConfig> registryConfigs = getRegistries();
        if (registryConfigs == null || registryConfigs.isEmpty()){
            throw new InitException("reg center address is empty");
        }
        SSFURL ssfurl = buildSSFURL(serverConfig);
        for (RegistryConfig config:registryConfigs){
            RegClient regClient = RegClientFactory.getRegClient(config.getRegisterAddresses());
            regClient.register(ssfurl);
        }
    }


    public void unRegisterFromRegCenter(ServerConfig serverConfig){
        SSFURL ssfurl = buildSSFURL(serverConfig);
        List<RegistryConfig> registryConfigs = getRegistries();
        for (RegistryConfig config:registryConfigs){
            RegClient regClient = RegClientFactory.getRegClient(config.getRegisterAddresses());
            regClient.unRegister(ssfurl);
        }
    }

     public SSFURL buildSSFURL(ServerConfig serverConfig){
        SSFURL ssfurl = new SSFURL();
        ssfurl.setIp(SSFContext.getLocalHost());
        ssfurl.setPid(SSFContext.getPID());
        ssfurl.setPort(serverConfig.getPort());
        ssfurl.setProtocol(ProtocolType.valueOf(serverConfig.getProtocol().toUpperCase()).getValue());
        ssfurl.setServiceName(getServiceInterfaceName());
        ssfurl.setAlias(getAlias());
        ssfurl.setStartTime(System.currentTimeMillis());
        return ssfurl;
    }

    @Override
    public String buildUniqueKey() {
        return "provider://"+serviceInterfaceName+":"+alias;
    }



}
