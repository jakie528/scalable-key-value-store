#include "include/Common.h"

class NodeManager {
   public:
    NodeManager();

    void RecvStopReq();
    void WaitUntilStop();

   private:
    mutex stop_mutex_;
    condition_variable stop_cv_;
    bool stop_signal_;
};