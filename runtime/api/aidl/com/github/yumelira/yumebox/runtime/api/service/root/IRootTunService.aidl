package com.github.yumelira.yumebox.runtime.api.service.root;

interface IRootTunService {
    String startRootTun(String requestJson);
    String restartRootTun(String requestJson);
    String reloadActiveProfile(String requestJson);
    String stopRootTun();
    String queryStatus();
    String queryTunnelStateJson();
    long queryTrafficNow();
    long queryTrafficTotal();
    String queryConnectionsJson();
    String queryAllProxyGroupsJson(boolean excludeNotSelectable);
    String queryProxyGroupNamesJson(boolean excludeNotSelectable);
    String queryProxyGroupJson(String name, String sort);
    String queryConfigurationJson();
    String queryProvidersJson();
    boolean patchSelector(String group, String name);
    boolean patchForceSelector(String group, String name);
    boolean closeConnection(String id);
    void closeAllConnections();
    String healthCheck(String group);
    String healthCheckProxy(String group, String proxyName);
    String updateProvider(String type, String name);
    void requestStop();
    String queryRecentLogsJson(long sinceSeq);
}
