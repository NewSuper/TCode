package com.qx.imui;


public class Constans {
    public static final int SDKAPPID = 1400440195;
    public static final String BASE_APP_KEY_TEST   = "3ccf3f154c944464a25771e870c0f32f";                                // 测试
    public static String BASE_APP_KEY              =  "AFwwDQYJKoCIhvcNAQEBBQADSwEw4AJBALWgVFXBO5W7aju9GzCRHlKkM5AMzEb";  //正试
    public static final String ERCODE = "http://reg.aitdcoin.com/#/download";
    public static final String SGP_APP = "http://download.sgpexchange.com/ ";
    public static String IMO_KEY = "EAD06EB2476A11C7";
    public static String airticalUrl = "https://download.aitdcoin.com/aitd/article?";
    public static String jintianUrl = "https://download.aitdcoin.com/aitd/recommendedAllowanceRule?";
    public static String BUSINESS_ARTICLE = "https://download.aitdcoin.com/aitd/article/businessArticle";  // 商学院

    public interface Key {
        int DEVICE_OS = 1;
        String publickey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC6xXyWXD5bHdmNGLmp2T6ZDgDi30tZhLUoIyHbstRCybnmnZ420qcF7hCHHMKKbjvAyYXeAZm95USF6zx0NIB1hOPlUswl0aWH7b23WFTcyY97NsLMIfnjU2SN3i8NPBfQslXT7zsU9f6aY5BIZWNu3IUdYSR8aBkBVjz2VYy29wIDAQAB";
    }

    public interface Conversation {
        static final int PictureRequestCode = 2003;
    }

    public final static int REQUEST_SUCCESS_CODE = 200;

    public static final String EXCHANGE_PWD_ERROR = "209";
    public static final String EXCHANGE_MONEY_NOT_ENOUGY = "204";
    public static final String REQUEST_OK = "200";
    public static final String REQUEST_ST001 = "ST001";//不符合参保条件
    public static final String REQUEST_ST601 = "601";
    public static final String REQUEST_ST602 = "602";
    public static final String REQUEST_STEB245 = "STEB245";//登录过期
    public static final String REQUEST_STEB217 = "STEB217";//用戶未登錄
    public static final String LIMIT_001 = "LIMIT_001";  // 您發布內容的數量已達到上限
    //设置默认超时时间
    public static final int DEFAULT_TIME = 3000;
    public static final int DEFAULT_TIME1 = 30000;
    public final static String Select_Key = "5287gexopi45956dtexuyhtng5986214";
    public static String A = "3412000-A";
    public static String B = "3001000-A";
    public static String C = "3101000-A";
    public static String D = "3101000-B";
    public static String tab = "全部";
    public static String tab22 = "待贖回";
    public static String live_title = "";
    public static String COUNTRY_NAME = "countryName";
    public static String COUNTRY_CODE = "countryCode";
    public static String HAVE_CERT = "haveCert";
    public static int REQUEST_TYPE = 0;
    // 健康评测
    public static String HEALTH_INFO = "health_info";
    public static String BAODAN_INFO = "baodan_info";
    public static String INSURANCE_TITLE = "insuranceTitle";
    public static String HAVE_HAVE = "1";
    public static int REQUEST_COUNTRY = 111;
    public static int RESULT_COUNTRY = 121;
    public static int num = 0;
    public static int num1 = 0;
    public static int num2 = 0;
    public static int num3 = 0;
    public static int num4 = 0;
    public static int num22 = 0;
    public static int tab_postion = 1;
    public static int tab_postion22 = 1;
    public static int groupNum = 0;
    public static String groupName = "";
    public static String save_img = "aitd/cache";
    public static String save_file = "/aitd/load/file";
    public static String save_emotion = "emition/img";
    public static String targetId;
    //  public static io.rong.imlib.model.Conversation.ConversationType conversationType;

    // 活体检测
    public static final int REQUEST_LIVENESS = 1000;
    public static String IMAGE_LIST = "image_list";
    public static String FIRST_CAIFU = "first_caifu";
    public static String FIRST_FIND = "first_find";
    public static String FIRST_ME = "first_me";
    //  public static DialogBean databean = new DialogBean();
    //  public static HealthListBean healthbean = new HealthListBean();
    //  public static HealthListBean healthbean1 = new HealthListBean();
    //  public static TouBaoBean touBaoBean = new TouBaoBean();
    //  public static OrderBean.DataBean.OrderInsuredPolicysOtherListBean othertoubaoBean = new OrderBean.DataBean.OrderInsuredPolicysOtherListBean();
    //  public static List<OrderBean.DataBean.OrderPolicyBenefitListBean> orderPolicyBenefitList = new ArrayList<>();
    public static final String BROADCAST_ACTION_DISC = "com.huke.socialcontact" + ".AnchorLiveActivity";
    public static final String logout = "logout";


    public static String INVATE_PERSON_URL = "https://reg.aitdcoin.com";//新環境正式
    public static String BASE_URL = "https://api.aitdcoin.com/";  //新環境正式
    public static String BASE_URL_TOUBAO = "https://api.aitdcoin.com/";  //新環境正式
    public static String BaseWebUrl1 = "https://workorder.aitdcoin.com";//新環境正式
    public static String BASE_LIVE = "https://api.aitdcoin.com/";//新環境正式
    public static String BASE_ZHIYA = "https://api.aitdcoin.com/";//新環境正式
    public static final String ServiceReceiver = "com.huke.socialcontact.upanddown";

    public static String BASE_KEFU_URL  = "https://customer.aitdcoin.com:443/im/text/0mnpw5.html";
    public static String upload_url     =  BASE_URL + "SocialFinance/file/upload/img/";
    public static String EVENT_URL      =  "http://datacenter-push-log.aitdcoin.com/";  //埋点上报
    public static String APP_ID         = "0e24af20-dfa9-4aa9-b56e-4bcbbcdac919";
    public static String APP_SECRET     = "267f56ce-f729-4f1e-ab6b-fdf4c8a612e4";
    //新IM
    public static String IM_APP_KEY     = "192ab05579ea421f9e8a1e1932732c45";
    public static String IM_APP_URL     = "https://qx-api.aitdcoin.com/";
    public static String NOTICE_OF_CLAIM="";

    public static void appVersionSwitch(int version) {
        if (version == VersionConstant.VERSION_TEST) {

            INVATE_PERSON_URL = "http://reg-test.aitdcoin.com";//新测试域名
            // BASE_URL = "https://api-test.aitdcoin.com/";  //新测试域名
            BASE_URL_TOUBAO = "http://api-test.aitdcoin.com/";  //新测试域名
            BaseWebUrl1 = "http://workorder-test.aitdcoin.com";//新测试域名
            //  BASE_LIVE = "https://api-test.aitdcoin.com/";//新测试域名

            BASE_LIVE = "http://api-test.aitdcoin.com/";//直播服务   todo  test 发版时删除
            BASE_URL = "http://api-test.aitdcoin.com/";  //用户服务  todo  test发版时删除

            BASE_ZHIYA = "http://api-test.aitdcoin.com/";//新测试域名
            BASE_KEFU_URL = "https://customer-test.aitdcoin.com/help/index.html";
            airticalUrl = "http://download-test.aitdcoin.com/aitd/article?";
            BUSINESS_ARTICLE = "https://download-test.aitdcoin.com/aitd/article/businessArticle";  // 商学院
            BASE_APP_KEY = BASE_APP_KEY_TEST;
            EVENT_URL      =  "http://point-upload-test.aitdcoin.com/";
            IM_APP_KEY     = "a7541e36a0414238a3ee92de482dac72";
            IM_APP_URL     = "https://qx-api-beta.aitdcoin.com/";
            NOTICE_OF_CLAIM= "https://download-test.aitdcoin.com/aitd/claimsNotice?registNo=";
        } else if (version == VersionConstant.VERSION_DEV) {
            INVATE_PERSON_URL = "https://reg-test.aitdcoin.com";//新开发域名
            BASE_URL = "https://api-dev.aitdcoin.com/";  //新开发域名
            BASE_URL_TOUBAO = "https://api-dev.aitdcoin.com/";  //新开发域名
            BaseWebUrl1 = "https://workorder-test.aitdcoin.com";//新开发域名
            BASE_LIVE = "https://api-dev.aitdcoin.com/";//新开发域名
            BASE_ZHIYA = "http://api-dev.aitdcoin.com/";//新开发域名
            BASE_KEFU_URL = "https://customer-test.aitdcoin.com/help/index.html";
            airticalUrl = "http://download-test.aitdcoin.com/aitd/article?";
            jintianUrl = "https://download-test.aitdcoin.com/aitd/recommendedAllowanceRule?";
            BASE_APP_KEY = BASE_APP_KEY_TEST;
            EVENT_URL      =  "http://point-upload-dev.aitdcoin.com/";
            IM_APP_KEY     = "a7541e36a0414238a3ee92de482dac72";
            IM_APP_URL     = "https://qx-api-beta.aitdcoin.com/";
            BUSINESS_ARTICLE = "https://download-test.aitdcoin.com/aitd/article/businessArticle";  // 商学院
            NOTICE_OF_CLAIM  = "https://download-test.aitdcoin.com/aitd/claimsNotice?registNo=";
        } else if (version == VersionConstant.VERSION_PRODUCE) {
            INVATE_PERSON_URL = "https://reg.hznixiya.com";   //新環境正式
            BASE_URL          = "https://api.hznixiya.com/";  //新環境正式
            BASE_URL_TOUBAO   = "https://api.hznixiya.com/";  //新環境正式
            BASE_LIVE         = "https://api.hznixiya.com/";//新環境正式
            BASE_ZHIYA        = "https://api.hznixiya.com/";//新環境正式
            BaseWebUrl1       = "https://workorder.hznixiya.com";//新環境正式
            BASE_KEFU_URL     = "https://customer.hznixiya.com/help/index.html";
            airticalUrl       = "https://download.hznixiya.com/aitd/article?";
            jintianUrl        = "https://download.hznixiya.com/aitd/recommendedAllowanceRule?";
            EVENT_URL         = "https://point-upload.hznixiya.com/";
            APP_ID            = "af3d1def-4e0b-4adb-a93b-e1363c21b4bf";
            APP_SECRET        = "da719b06-07c2-4bd9-bc18-136c7acd0c15";
            IM_APP_KEY        = "192ab05579ea421f9e8a1e1932732c45";
            IM_APP_URL        = "https://qx-api.hznixiya.com/";
            BUSINESS_ARTICLE  = "https://download.hznixiya.com/aitd/article/businessArticle";  // 商学院
            NOTICE_OF_CLAIM   = "https://download.hznixiya.com/aitd/claimsNotice?registNo=";
        } else if (version == VersionConstant.VERSION_PRE_ENVIROMENT) {  // 预发布服务地址 www-pre.sgpexchange.com
            INVATE_PERSON_URL = "http://reg-pre.aitdcoin.com";//新开发域名
            BASE_URL = "http://api-pre.aitdcoin.com/";  //新开发域名
            BASE_URL_TOUBAO = "http://api-pre.aitdcoin.com/";  //新开发域名
            BaseWebUrl1 = "http://api-pre.aitdcoin.com";//新开发域名
            BASE_LIVE = "http://api-pre.aitdcoin.com/";//新开发域名
            BASE_ZHIYA = "http://api-pre.aitdcoin.com/";//新开发域名
            BASE_KEFU_URL = "https://customer.aitdcoin.com:443/im/text/0mnpw5.html";
            airticalUrl = "https://download-pre.aitdcoin.com/aitd/article?";
            jintianUrl = "https://download-pre.aitdcoin.com/aitd/recommendedAllowanceRule?";
            BASE_APP_KEY = BASE_APP_KEY_TEST;
            EVENT_URL      = "http://point-upload-pre.aitdcoin.com/";
            APP_ID         = "9c52a9db-0ecc-48bc-a78d-4c823d292533";
            APP_SECRET     = "755abfa1-2663-455f-a76a-d344e9e9c4eb";
            IM_APP_KEY     = "1307f884c42342d1841e0363179f59c9"; //"b51702d42bf54fcb9d1624227f8ea637";
            IM_APP_URL     = "https://qx-api-beta.aitdcoin.com/";
            BUSINESS_ARTICLE = "https://download-pre.aitdcoin.com/aitd/article/businessArticle";  // 商学院
            NOTICE_OF_CLAIM  = "https://download-pre.aitdcoin.com/aitd/claimsNotice?registNo=";
        }
    }
}

