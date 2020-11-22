package com.example.map.testapp;

import java.util.ArrayList;
import java.util.List;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.bikenavi.params.BikeRouteNodeInfo;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.mapapi.walknavi.params.WalkRouteNodeInfo;

import com.baidu.location.BDLocation;
import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
public class BNaviMainActivity extends Activity  implements OnGetRoutePlanResultListener {

    private final static String TAG = BNaviMainActivity.class.getSimpleName();
    private BDLocation locationService;
    public LocationClient mLocationClient = null;

    private MapView mMapView;
    private BaiduMap mBaiduMap;

    /*导航起终点Marker，可拖动改变起终点的坐标*/
    private Marker mStartMarker;
    private Marker mEndMarker;
    private RoutePlanSearch routePlanSearch = null;


    private LatLng startPt;
    private LatLng endPt;

    private BikeNaviLaunchParam bikeParam;
    private WalkNaviLaunchParam walkParam;

    private static boolean isPermissionRequested = false;

    private DrivingRouteResult mDrivingRouteResult = null;
    private BitmapDescriptor bdStart = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_start);
    private BitmapDescriptor bdEnd = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_end);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_main);
        requestPermission();
        mMapView = (MapView) findViewById(R.id.mapview);
        initMapStatus();

        /*导航*/
        Button btn_bikenavi = (Button) findViewById(R.id.btn_bikenavi);
        btn_bikenavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBikeNavi();
            }
        });

        /*规划线路*/
        Button btn_guid = (Button) findViewById(R.id.btn_guid);
        btn_guid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guiHua(startPt,endPt);
            }
        });

        /*重新定位*/
        Button arWalkBtn = (Button) findViewById(R.id.btn_loca);
        arWalkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloca = true;
            }
        });
        startPt = new LatLng(40.057038,116.307899);
        endPt = new LatLng(40.035916, 116.340722);
        initOverlay();
    }

    private boolean isfirst = true,reloca = false;
    private void initMapStatus(){
        mBaiduMap = mMapView.getMap();
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(new LatLng(40.048424, 116.313513)).zoom(15);
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        BDLocationListener bdLocationListener = new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation location) {
                double latitude = location.getLatitude();    //获取纬度信息
                double longitude = location.getLongitude();    //获取经度信息
                float radius = location.getRadius();    //获取定位精度，默认值为0.0f
                String coorType = location.getCoorType();
                int errorCode = location.getLocType();
                Log.d(TAG, "onReceiveLocation:latitude " + latitude);
                Log.d(TAG, "onReceiveLocation:longitude " + longitude);
                if(isfirst ) {
                    startPt =new LatLng(latitude,longitude);

                    endPt =new LatLng(latitude,longitude+0.03);
                    mBaiduMap = mMapView.getMap();
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target( new LatLng(latitude,longitude));
                    mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                    isfirst=false;
                    // 起点坐标
                    MarkerOptions ooA = new MarkerOptions().position( new LatLng(latitude,longitude)).icon(bdStart)
                            .zIndex(9).draggable(true);
                    mStartMarker = (Marker) (mBaiduMap.addOverlay(ooA));
                    mStartMarker.setDraggable(true);
                    // 终点坐标
                    MarkerOptions ooB = new MarkerOptions().position(endPt).icon(bdEnd)
                            .zIndex(5);
                    mEndMarker = (Marker) (mBaiduMap.addOverlay(ooB));
                    mEndMarker.setDraggable(true);

                    BikeRouteNodeInfo bikeStartNode = new BikeRouteNodeInfo();
                    bikeStartNode.setLocation(startPt);
                    BikeRouteNodeInfo bikeEndNode = new BikeRouteNodeInfo();
                    bikeEndNode.setLocation(endPt);
                    bikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartNode).endNodeInfo(bikeEndNode);
                }
                if(reloca) {
                    reloca = false;
                    startPt =new LatLng(latitude,longitude);
                    mStartMarker.setPosition( new LatLng(latitude,longitude));
                    BikeRouteNodeInfo bikeStartNode = new BikeRouteNodeInfo();
                    bikeStartNode.setLocation(startPt);
                    BikeRouteNodeInfo bikeEndNode = new BikeRouteNodeInfo();
                    bikeEndNode.setLocation(endPt);
                    bikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartNode).endNodeInfo(bikeEndNode);
                }
            }
        };
        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(bdLocationListener);
        LocationClientOption option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);

        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGps(false);
        option.setNeedNewVersionRgc(true);
        mLocationClient.setLocOption(option);
        mLocationClient.start();

    }

    /**
     * 初始化导航起终点Marker
     */
    public void initOverlay() {
        mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            public void onMarkerDrag(Marker marker) {
            }

            public void onMarkerDragEnd(Marker marker) {
                if(marker == mStartMarker){
                    startPt = marker.getPosition();
                }else if(marker == mEndMarker){
                    endPt = marker.getPosition();
                }

                BikeRouteNodeInfo bikeStartNode = new BikeRouteNodeInfo();
                bikeStartNode.setLocation(startPt);
                BikeRouteNodeInfo bikeEndNode = new BikeRouteNodeInfo();
                bikeEndNode.setLocation(endPt);
                bikeParam = new BikeNaviLaunchParam().startNodeInfo(bikeStartNode).endNodeInfo(bikeEndNode);

            }

            public void onMarkerDragStart(Marker marker) {
            }
        });
    }

    /**
     * 开始骑行导航
     */
    private void startBikeNavi() {
        Log.d(TAG, "startBikeNavi");
        try {
            BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "BikeNavi engineInitSuccess");
                    routePlanWithBikeParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "BikeNavi engineInitFail");
                    BikeNavigateHelper.getInstance().unInitNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 开始步行导航
     */
    private void guiHua(LatLng start,LatLng end){
        if (routePlanSearch==null) {
            routePlanSearch=RoutePlanSearch.newInstance();

            routePlanSearch.setOnGetRoutePlanResultListener( this);
        }
        DrivingRoutePlanOption option=new DrivingRoutePlanOption();

        option.from(PlanNode.withLocation(start))
                .to(PlanNode.withLocation(end));
        routePlanSearch.drivingSearch(option);

    }


    /**
     * 发起骑行导航算路
     */
    private void routePlanWithBikeParam() {
        BikeNavigateHelper.getInstance().routePlanWithRouteNode(bikeParam, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d(TAG, "BikeNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(BNaviMainActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d(TAG, "BikeNavi onRoutePlanFail");
            }

        });
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanWithWalkParam() {
        WalkNavigateHelper.getInstance().routePlanWithRouteNode(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {

                Log.d(TAG, "onRoutePlanSuccess");

                Intent intent = new Intent();
                intent.setClass(BNaviMainActivity.this, WNaviGuideActivity.class);
                startActivity(intent);

            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    /**
     * Android6.0之后需要动态申请权限
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissionsList = new ArrayList<>();

            String[] permissions = {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.WRITE_SETTINGS,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_MULTICAST_STATE


            };

            for (String perm : permissions) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(perm)) {
                    permissionsList.add(perm);
                    // 进入到这里代表没有权限.
                }
            }

            if (permissionsList.isEmpty()) {
                return;
            } else {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]), 0);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        bdStart.recycle();
        bdEnd.recycle();
    }


    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {

    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {

    }

    @Override
    public void onGetMassTransitRouteResult(MassTransitRouteResult result) {

    }

    /**
     * 驾车路线结果回调
     *
     * @param result 驾车路线结果
     */
    boolean hasShowDialog = false;
    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result != null && result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            Toast.makeText(BNaviMainActivity.this, "起终点或途经点地址有岐义,通过 result.getSuggestAddrInfo()接口获取建议查询信息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            Toast.makeText(BNaviMainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            if (result.getRouteLines().size() > 1) {
                mDrivingRouteResult = result;
                if (!hasShowDialog) {
                    // 多条路线Dialog
                    
                    SelectRouteDialog selectRouteDialog = new SelectRouteDialog(BNaviMainActivity.this,
                            result.getRouteLines(), RouteLineAdapter.Type.DRIVING_ROUTE);
                    selectRouteDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            hasShowDialog = false;
                        }
                    });
                    selectRouteDialog.setOnItemInDlgClickLinster(new SelectRouteDialog.OnItemInDlgClickListener() {
                        public void onItemClick(int position) {
                            // 获取选中的路线
                            DrivingRouteLine mRouteLine = mDrivingRouteResult.getRouteLines().get(position);
                            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
                            mBaiduMap.setOnMarkerClickListener(overlay);
                            DrivingRouteOverlay mRouteOverlay = overlay;
                            overlay.setData(mDrivingRouteResult.getRouteLines().get(position));
                            overlay.addToMap();
                            overlay.zoomToSpan();
                        }

                    });
                    selectRouteDialog.show();
                    hasShowDialog = true;
                }
            } else if (result.getRouteLines().size() == 1) {
                DrivingRouteLine mRouteLine = result.getRouteLines().get(0);
                DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
                DrivingRouteOverlay mRouteOverlay = overlay;
                mBaiduMap.setOnMarkerClickListener(overlay);
                overlay.setData(result.getRouteLines().get(0));
                overlay.addToMap();
                overlay.zoomToSpan();
            } else {
                Log.d("route result", "结果数<0");
                return;
            }
        }
    }

    @Override
    public void onGetIndoorRouteResult(IndoorRouteResult result) {

    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult result) {

    }

}
